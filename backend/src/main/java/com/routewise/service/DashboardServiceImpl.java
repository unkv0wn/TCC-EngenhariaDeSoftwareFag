package com.routewise.service;

import com.routewise.dto.DashboardDto;
import com.routewise.entity.Order;
import com.routewise.entity.Refueling;
import com.routewise.entity.SavedRoute;
import com.routewise.entity.Vehicle;
import com.routewise.repository.OrderRepository;
import com.routewise.repository.RefuelingRepository;
import com.routewise.repository.SavedRouteRepository;
import com.routewise.service.RouteCostEstimator.CostBreakdown;
import com.routewise.validation.VehicleProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 *       quando existem. Dado — e medido na frota inteira, não na janela da tela: cinco
 *       abastecimentos descrevem o veículo, e ignorá-los por serem antigos trocaria uma
 *       medição por uma constante. É também o que a tela de cada rota usa.</li>
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
  private final RouteStopResolver stopResolver;
  private final RouteCostEstimator costEstimator;
  private final RouteCostingService costingService;

  public DashboardServiceImpl(
    SavedRouteRepository routeRepository,
    OrderRepository orderRepository,
    RefuelingRepository refuelingRepository,
    RouteStopResolver stopResolver,
    RouteCostEstimator costEstimator,
    RouteCostingService costingService
  ) {
    this.routeRepository = routeRepository;
    this.orderRepository = orderRepository;
    this.refuelingRepository = refuelingRepository;
    this.stopResolver = stopResolver;
    this.costEstimator = costEstimator;
    this.costingService = costingService;
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

    // Consumo, preço e carga vêm do RouteCostingService, que é o mesmo caminho usado pela
    // tela de uma rota só. Montá-los aqui também faria o total do dashboard divergir da
    // soma das rotas que ele conta no dia em que uma das regras mudasse.
    RouteCostingService.FleetInputs inputs = costingService.inputsFor(routes);
    Map<UUID, Vehicle> vehicles = inputs.vehicles();

    Map<UUID, List<Refueling>> refuelingsByVehicle = allRefuelings.stream()
      .collect(Collectors.groupingBy(Refueling::getVehicleId));

    // O que a frota de fato gastou na janela — diferente do preço usado no custeio, que é
    // o da frota inteira. Esta linha responde "quanto saiu do caixa nestes dias".
    List<Refueling> inWindow = allRefuelings.stream()
      .filter(refueling -> !refueling.getDate().isBefore(fromDate))
      .toList();
    double refueledLiters = inWindow.stream().mapToDouble(Refueling::getLitersRefueled).sum();
    double refueledCost = inWindow.stream()
      .mapToDouble(r -> r.getLitersRefueled() * r.getPricePerLiter())
      .sum();

    CostBreakdown cost = CostBreakdown.zero();
    Map<UUID, Double> distanceByVehicle = new HashMap<>();
    int stops = 0;

    for (SavedRoute route : routes) {
      cost = cost.plus(costingService.costOf(route, inputs).breakdown());
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
        inputs.fuelPricePerLiter(),
        inputs.fuelPriceMeasured()
      ),
      vehicleConsumptions(
        vehicles, refuelingsByVehicle, inputs.consumptionByVehicle(), distanceByVehicle
      ),
      dailyDeliveries(delivered, fromDate, today)
    );
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
