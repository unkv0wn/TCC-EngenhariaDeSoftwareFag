package com.routewise.service;

import com.routewise.dto.DashboardDto;
import com.routewise.entity.Order;
import com.routewise.entity.OrderItem;
import com.routewise.entity.Product;
import com.routewise.entity.Refueling;
import com.routewise.entity.SavedRoute;
import com.routewise.entity.Vehicle;
import com.routewise.repository.OrderRepository;
import com.routewise.repository.ProductRepository;
import com.routewise.repository.RefuelingRepository;
import com.routewise.repository.SavedRouteRepository;
import com.routewise.repository.VehicleRepository;
import com.routewise.service.RouteCostEstimator.CostBreakdown;
import com.routewise.validation.EmpiricalFuelConsumptionCalculator;
import com.routewise.validation.EmpiricalFuelConsumptionCalculator.RefuelingRecord;
import com.routewise.validation.VehicleProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta a tela inicial do gestor.
 *
 * <p>Esta tela mostrava quatro números escritos à mão no componente — "12 rotas", "184
 * km" — ao lado de um aviso dizendo que o espaço seria definido depois. Era a única
 * parte do sistema que mentia, e a primeira que alguém vê depois de entrar.
 *
 * <p>O que ela mostra agora vem de três fontes, e a diferença entre elas importa:
 *
 * <ul>
 *   <li><strong>Operação</strong> — contagem direta de rotas, entregas e pedidos. Dado.</li>
 *   <li><strong>Consumo e preço</strong> — medidos nos abastecimentos da própria frota,
 *       quando existem. Dado, com janela móvel.</li>
 *   <li><strong>Custo</strong> — estimativa do modelo do TCC
 *       ({@link RouteCostEstimator}), não despesa registrada. Pneu e hora de motorista
 *       são constantes documentadas do experimento, porque o sistema não cadastra troca
 *       de pneu nem valor-hora.</li>
 * </ul>
 *
 * <p>Por isso o DTO devolve o abastecimento real ao lado do custo estimado, em vez de
 * só a estimativa: são números diferentes e a tela diz qual é qual. Eles também não
 * fecham entre si, e não deveriam — o caminhão abastece para rodar tudo, inclusive o
 * que nunca virou rota aqui dentro.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements IDashboardService {

  /** Faixa padrão quando a tela não pede outra. */
  public static final int DEFAULT_DAYS = 30;

  private static final int MAX_DAYS = 365;

  /** Fuso em que "hoje" é hoje — o relatório é lido no horário da operação, não em UTC. */
  private static final ZoneId ZONE = ZoneId.systemDefault();

  private final SavedRouteRepository routeRepository;
  private final OrderRepository orderRepository;
  private final RefuelingRepository refuelingRepository;
  private final VehicleRepository vehicleRepository;
  private final ProductRepository productRepository;
  private final RouteStopResolver stopResolver;
  private final RouteCostEstimator costEstimator;

  public DashboardServiceImpl(
    SavedRouteRepository routeRepository,
    OrderRepository orderRepository,
    RefuelingRepository refuelingRepository,
    VehicleRepository vehicleRepository,
    ProductRepository productRepository,
    RouteStopResolver stopResolver,
    RouteCostEstimator costEstimator
  ) {
    this.routeRepository = routeRepository;
    this.orderRepository = orderRepository;
    this.refuelingRepository = refuelingRepository;
    this.vehicleRepository = vehicleRepository;
    this.productRepository = productRepository;
    this.stopResolver = stopResolver;
    this.costEstimator = costEstimator;
  }

  @Override
  public DashboardDto summary(int days) {
    int window = Math.clamp(days, 1, MAX_DAYS);
    LocalDate today = LocalDate.now(ZONE);
    LocalDate fromDate = today.minusDays(window - 1L);
    Instant from = fromDate.atStartOfDay(ZONE).toInstant();

    List<SavedRoute> routes = routeRepository.findByCreatedAtGreaterThanEqual(from);
    List<Order> delivered = orderRepository.findByDeliveredAtGreaterThanEqual(from);
    List<Refueling> allRefuelings = refuelingRepository.findAll();
    Map<UUID, Vehicle> vehicles = vehicleRepository.findAll().stream()
      .collect(Collectors.toMap(Vehicle::getId, Function.identity()));

    // Consumo medido por veículo, da janela móvel do calculador empírico. Vale para a
    // frota inteira, não só para a janela do relatório: cinco abastecimentos descrevem o
    // veículo, e descartá-los por serem de 40 dias atrás seria jogar fora a medição para
    // usar uma constante.
    Map<UUID, List<Refueling>> refuelingsByVehicle = allRefuelings.stream()
      .collect(Collectors.groupingBy(Refueling::getVehicleId));

    Map<UUID, Double> consumptionByVehicle = new HashMap<>();
    for (Map.Entry<UUID, List<Refueling>> entry : refuelingsByVehicle.entrySet()) {
      VehicleProfile profile = costEstimator.profileFor(vehicles.get(entry.getKey()));
      consumptionByVehicle.put(entry.getKey(), measuredConsumption(entry.getValue(), profile));
    }

    List<Refueling> inWindow = allRefuelings.stream()
      .filter(refueling -> !refueling.getDate().isBefore(fromDate))
      .toList();
    double refueledLiters = inWindow.stream().mapToDouble(Refueling::getLitersRefueled).sum();
    double refueledCost = inWindow.stream()
      .mapToDouble(r -> r.getLitersRefueled() * r.getPricePerLiter())
      .sum();

    // Preço médio ponderado pelos litros: um abastecimento de 200 L pesa mais do que um
    // de 20 L na conta de quanto a frota paga por litro.
    boolean priceMeasured = refueledLiters > 0;
    double fuelPrice = priceMeasured
      ? refueledCost / refueledLiters
      : VehicleProfile.defaultProfile().fuelPricePerLiter();

    Map<UUID, Double> weightByOrder = cargoWeightByOrder(routes);

    CostBreakdown cost = CostBreakdown.zero();
    Map<UUID, Double> distanceByVehicle = new HashMap<>();
    int stops = 0;

    for (SavedRoute route : routes) {
      Vehicle vehicle = vehicles.get(route.getVehicleId());
      VehicleProfile profile = costEstimator.profileFor(vehicle);
      double consumption = consumptionByVehicle.getOrDefault(
        route.getVehicleId(), profile.baseFuelConsumptionLPer100Km()
      );

      double cargoKg = stopResolver.allOrderIds(route).stream()
        .mapToDouble(orderId -> weightByOrder.getOrDefault(orderId, 0.0))
        .sum();

      cost = cost.plus(costEstimator.estimate(
        route.getTotalDistanceKm(),
        route.getTotalDurationMin(),
        cargoKg,
        profile,
        consumption,
        fuelPrice
      ));

      stops += stopResolver.countStops(route);
      distanceByVehicle.merge(route.getVehicleId(), route.getTotalDistanceKm(), Double::sum);
    }

    double distanceKm = routes.stream().mapToDouble(SavedRoute::getTotalDistanceKm).sum();

    return new DashboardDto(
      new DashboardDto.Period(window, fromDate, today),
      new DashboardDto.Operation(
        routes.size(),
        (int) routes.stream().filter(r -> RouteStatus.CONCLUIDA.equals(r.getStatus())).count(),
        distanceKm,
        routes.stream().mapToDouble(SavedRoute::getTotalDurationMin).sum(),
        stops,
        delivered.size(),
        (int) orderRepository.countByStatus(RouteOrderLifecycle.FATURADO)
      ),
      new DashboardDto.Cost(
        cost.fuelLiters(),
        cost.fuelCost(),
        cost.tireCost(),
        cost.driverCost(),
        cost.total(),
        distanceKm > 0 ? cost.total() / distanceKm : 0.0,
        refueledLiters,
        refueledCost,
        fuelPrice,
        priceMeasured
      ),
      vehicleConsumptions(vehicles, refuelingsByVehicle, consumptionByVehicle, distanceByVehicle),
      dailyDeliveries(delivered, fromDate, today)
    );
  }

  /**
   * Consumo medido nos abastecimentos do veículo, ou a constante do perfil.
   *
   * <p>O {@code kmSincePrevious} de cada abastecimento é derivado aqui, e não lido de
   * uma coluna: a migração {@code V7} deixou esse campo fora da tabela de propósito,
   * para que editar um registro antigo não deixe os seguintes desatualizados. Ordena-se
   * por odômetro, e a diferença para o anterior é a distância daquele tanque.
   */
  private double measuredConsumption(List<Refueling> refuelings, VehicleProfile profile) {
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

  /** Peso de cada pedido que alguma rota da janela entrega, somando item × peso do produto. */
  private Map<UUID, Double> cargoWeightByOrder(List<SavedRoute> routes) {
    List<UUID> orderIds = routes.stream().flatMap(route -> stopResolver.allOrderIds(route).stream()).distinct().toList();
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

  private List<DashboardDto.VehicleConsumption> vehicleConsumptions(
    Map<UUID, Vehicle> vehicles,
    Map<UUID, List<Refueling>> refuelingsByVehicle,
    Map<UUID, Double> consumptionByVehicle,
    Map<UUID, Double> distanceByVehicle
  ) {
    return vehicles.values().stream()
      .map(vehicle -> {
        VehicleProfile profile = costEstimator.profileFor(vehicle);
        int refuelings = refuelingsByVehicle.getOrDefault(vehicle.getId(), List.of()).size();
        // Um abastecimento sozinho não mede nada: sem o anterior, não há distância
        // percorrida com aquele tanque.
        boolean measured = refuelings >= 2 && consumptionByVehicle.containsKey(vehicle.getId());
        return new DashboardDto.VehicleConsumption(
          vehicle.getPlate(),
          profile.label(),
          measured
            ? consumptionByVehicle.get(vehicle.getId())
            : profile.baseFuelConsumptionLPer100Km(),
          measured,
          refuelings,
          distanceByVehicle.getOrDefault(vehicle.getId(), 0.0)
        );
      })
      .sorted(Comparator.comparingDouble(DashboardDto.VehicleConsumption::routeDistanceKm).reversed())
      .toList();
  }

  /** Série diária, com os dias sem entrega presentes e zerados — senão o gráfico mente sobre o ritmo. */
  private List<DashboardDto.DailyDeliveries> dailyDeliveries(List<Order> delivered, LocalDate from, LocalDate to) {
    Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
    for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
      byDay.put(day, 0);
    }
    for (Order order : delivered) {
      LocalDate day = order.getDeliveredAt().atZone(ZONE).toLocalDate();
      byDay.computeIfPresent(day, (key, count) -> count + 1);
    }
    return byDay.entrySet().stream()
      .map(entry -> new DashboardDto.DailyDeliveries(entry.getKey(), entry.getValue()))
      .toList();
  }
}
