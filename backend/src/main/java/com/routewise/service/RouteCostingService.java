package com.routewise.service;

import com.routewise.entity.Order;
import com.routewise.entity.Product;
import com.routewise.entity.Refueling;
import com.routewise.entity.SavedRoute;
import com.routewise.entity.Vehicle;
import com.routewise.repository.OrderRepository;
import com.routewise.repository.ProductRepository;
import com.routewise.repository.RefuelingRepository;
import com.routewise.repository.VehicleRepository;
import com.routewise.service.RouteCostEstimator.CostBreakdown;
import com.routewise.validation.EmpiricalFuelConsumptionCalculator;
import com.routewise.validation.EmpiricalFuelConsumptionCalculator.RefuelingRecord;
import com.routewise.validation.VehicleProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta os insumos que o {@link RouteCostEstimator} precisa e custeia rotas com eles.
 *
 * <p>Existe porque a mesma rota é custeada em dois lugares: somada às outras no
 * dashboard, e sozinha na própria tela. O estimador recebe consumo, preço e carga
 * prontos — de propósito, para ser testável sem banco —, então <em>alguém</em> tem de
 * buscá-los. Esse alguém era o {@code DashboardServiceImpl}; repetir a busca na tela da
 * rota faria as duas respostas divergirem no dia em que uma das regras mudasse, e é
 * exatamente assim que as duas máquinas de status deste projeto já saíram de sincronia.
 *
 * <p>A regra de cada insumo, e por quê:
 *
 * <ul>
 *   <li><strong>Consumo</strong> — medido nos abastecimentos do próprio veículo, pela
 *       janela móvel do {@link EmpiricalFuelConsumptionCalculator}. Vale para a frota
 *       inteira e não para a janela do relatório: cinco abastecimentos descrevem o
 *       veículo, e descartá-los por serem antigos seria jogar fora a medição para usar
 *       uma constante.</li>
 *   <li><strong>Preço do litro</strong> — média de <em>todos</em> os abastecimentos,
 *       ponderada pelos litros, pelo mesmo motivo. É o preço da frota, não o preço do
 *       dia da rota: uma rota de março é custeada com o preço médio que a frota pagou,
 *       o que subestima ou superestima conforme o combustível andou desde então.</li>
 *   <li><strong>Carga</strong> — soma de item × peso do produto, pelos pedidos que a
 *       rota entrega. Rota salva antes de {@code LabeledStop.orderIds} existir não tem
 *       vínculo com pedido, então pesa zero e aparece como caminhão vazio.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class RouteCostingService {

  /**
   * Insumos da frota, buscados uma vez e reusados por rota.
   *
   * <p>Separado do custeio porque custear N rotas não deve fazer N buscas: o dashboard
   * monta isto uma vez para a janela inteira.
   *
   * @param vehicles             veículos por id, para achar o perfil de cada rota
   * @param consumptionByVehicle consumo medido (L/100km) dos veículos que têm medição
   * @param fuelPricePerLiter    preço a usar — o da frota quando medido, senão a constante
   * @param fuelPriceMeasured    se o preço acima veio de abastecimento ou da constante
   * @param cargoWeightByOrder   peso por pedido, dos pedidos que as rotas entregam
   */
  public record FleetInputs(
    Map<UUID, Vehicle> vehicles,
    Map<UUID, Double> consumptionByVehicle,
    double fuelPricePerLiter,
    boolean fuelPriceMeasured,
    Map<UUID, Double> cargoWeightByOrder
  ) {}

  /**
   * Custo de uma rota com o contexto que o explica.
   *
   * <p>Os dois {@code measured} não são detalhe de implementação: o mesmo total significa
   * coisas diferentes se o consumo foi medido no veículo ou herdado de um perfil, e a tela
   * precisa poder dizer qual dos dois.
   */
  public record RouteCost(
    CostBreakdown breakdown,
    VehicleProfile profile,
    double consumptionLPer100Km,
    boolean consumptionMeasured,
    double fuelPricePerLiter,
    boolean fuelPriceMeasured,
    double cargoWeightKg
  ) {}

  /** Abaixo disto o calculador empírico não tem trecho para medir: um tanque só não dá consumo. */
  private static final int MIN_REFUELINGS_TO_MEASURE = 2;

  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final RefuelingRepository refuelingRepository;
  private final VehicleRepository vehicleRepository;
  private final RouteStopResolver stopResolver;
  private final RouteCostEstimator costEstimator;

  public RouteCostingService(
    OrderRepository orderRepository,
    ProductRepository productRepository,
    RefuelingRepository refuelingRepository,
    VehicleRepository vehicleRepository,
    RouteStopResolver stopResolver,
    RouteCostEstimator costEstimator
  ) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
    this.refuelingRepository = refuelingRepository;
    this.vehicleRepository = vehicleRepository;
    this.stopResolver = stopResolver;
    this.costEstimator = costEstimator;
  }

  /** Insumos para custear este conjunto de rotas. */
  public FleetInputs inputsFor(List<SavedRoute> routes) {
    Map<UUID, Vehicle> vehicles = vehicleRepository.findAll().stream()
      .collect(Collectors.toMap(Vehicle::getId, Function.identity()));

    List<Refueling> allRefuelings = refuelingRepository.findAll();
    Map<UUID, List<Refueling>> byVehicle = allRefuelings.stream()
      .collect(Collectors.groupingBy(Refueling::getVehicleId));

    Map<UUID, Double> consumptionByVehicle = new HashMap<>();
    for (Map.Entry<UUID, List<Refueling>> entry : byVehicle.entrySet()) {
      if (entry.getValue().size() < MIN_REFUELINGS_TO_MEASURE) continue;
      VehicleProfile profile = costEstimator.profileFor(vehicles.get(entry.getKey()));
      consumptionByVehicle.put(entry.getKey(), measuredConsumption(entry.getValue(), profile));
    }

    double litersTotal = allRefuelings.stream().mapToDouble(Refueling::getLitersRefueled).sum();
    boolean priceMeasured = litersTotal > 0;
    // Ponderado pelos litros: um abastecimento de 200 L pesa mais do que um de 20 L na
    // conta de quanto a frota paga por litro.
    double fuelPrice = priceMeasured
      ? allRefuelings.stream().mapToDouble(r -> r.getLitersRefueled() * r.getPricePerLiter()).sum() / litersTotal
      : VehicleProfile.defaultProfile().fuelPricePerLiter();

    return new FleetInputs(
      vehicles, consumptionByVehicle, fuelPrice, priceMeasured, cargoWeightByOrder(routes)
    );
  }

  /** Custo de uma rota, com insumos já montados. */
  public RouteCost costOf(SavedRoute route, FleetInputs inputs) {
    Vehicle vehicle = inputs.vehicles().get(route.getVehicleId());
    VehicleProfile profile = costEstimator.profileFor(vehicle);

    Double measured = inputs.consumptionByVehicle().get(route.getVehicleId());
    double consumption = measured != null ? measured : profile.baseFuelConsumptionLPer100Km();

    double cargoKg = stopResolver.allOrderIds(route).stream()
      .mapToDouble(orderId -> inputs.cargoWeightByOrder().getOrDefault(orderId, 0.0))
      .sum();

    CostBreakdown breakdown = costEstimator.estimate(
      route.getTotalDistanceKm(),
      route.getTotalDurationMin(),
      cargoKg,
      profile,
      consumption,
      inputs.fuelPricePerLiter()
    );

    return new RouteCost(
      breakdown,
      profile,
      consumption,
      measured != null,
      inputs.fuelPricePerLiter(),
      inputs.fuelPriceMeasured(),
      cargoKg
    );
  }

  /** Custo de uma rota sozinha — busca os insumos só para ela. */
  public RouteCost costOf(SavedRoute route) {
    return costOf(route, inputsFor(List.of(route)));
  }

  /**
   * Consumo medido nos abastecimentos do veículo, ou a constante do perfil.
   *
   * <p>O {@code kmSincePrevious} de cada abastecimento é derivado aqui, e não lido de uma
   * coluna: a migração {@code V7} deixou esse campo fora da tabela de propósito, para que
   * editar um registro antigo não deixe os seguintes desatualizados. Ordena-se por
   * odômetro, e a diferença para o anterior é a distância daquele tanque.
   */
  double measuredConsumption(List<Refueling> refuelings, VehicleProfile profile) {
    List<Refueling> ordered = refuelings.stream()
      .sorted(Comparator.comparingDouble(Refueling::getOdometerKm))
      .toList();

    List<RefuelingRecord> log = new ArrayList<>();
    for (int i = 1; i < ordered.size(); i++) {
      double km = ordered.get(i).getOdometerKm() - ordered.get(i - 1).getOdometerKm();
      // Odômetro que não andou, ou andou para trás, é erro de digitação — e dividir por
      // ele daria consumo infinito, que contaminaria a média de toda a frota.
      if (km <= 0) continue;
      log.add(new RefuelingRecord(
        ordered.get(i).getDate().toString(),
        ordered.get(i).getLitersRefueled(),
        km
      ));
    }

    return EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
      log, profile.baseFuelConsumptionLPer100Km()
    );
  }

  /** Peso de cada pedido que alguma das rotas entrega, somando item × peso do produto. */
  private Map<UUID, Double> cargoWeightByOrder(List<SavedRoute> routes) {
    List<UUID> orderIds = routes.stream()
      .flatMap(route -> stopResolver.allOrderIds(route).stream())
      .distinct()
      .toList();
    if (orderIds.isEmpty()) return Map.of();

    Map<UUID, Double> productWeights = productRepository.findAll().stream()
      .collect(Collectors.toMap(Product::getId, Product::getWeightKg));

    return orderRepository.findAllById(orderIds).stream().collect(Collectors.toMap(
      Order::getId,
      order -> order.getItems().stream()
        .mapToDouble(item -> item.getQuantity() * productWeights.getOrDefault(item.getProductId(), 0.0))
        .sum()
    ));
  }
}
