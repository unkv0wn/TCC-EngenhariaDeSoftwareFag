package com.routewise.service;

import com.routewise.dto.IntegrationDeliveryDto;
import com.routewise.dto.IntegrationOrderDto;
import com.routewise.dto.IntegrationRouteDto;
import com.routewise.entity.Customer;
import com.routewise.entity.Driver;
import com.routewise.entity.Order;
import com.routewise.entity.OrderItem;
import com.routewise.entity.RouteEvent;
import com.routewise.entity.SavedRoute;
import com.routewise.entity.Vehicle;
import com.routewise.repository.CustomerRepository;
import com.routewise.repository.DriverRepository;
import com.routewise.repository.OrderRepository;
import com.routewise.repository.RouteEventRepository;
import com.routewise.repository.SavedRouteRepository;
import com.routewise.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Os dados que a superfície de integração expõe.
 *
 * <p>Classe própria, e não os serviços de CRUD existentes, pelo mesmo motivo que os DTOs
 * são próprios: o que sai daqui é um contrato com terceiros. Chamando
 * {@code OrderServiceImpl} direto, qualquer mudança feita para atender uma tela do painel
 * vazaria para quem integrou.
 *
 * <p>Somente leitura — a escrita externa (um ERP criando pedido) ficou fora deste escopo
 * de propósito: ela traz validação e idempotência que merecem decisão própria.
 */
@Service
@Transactional(readOnly = true)
public class IntegrationDataService {

  private final OrderRepository orderRepository;
  private final CustomerRepository customerRepository;
  private final SavedRouteRepository routeRepository;
  private final DriverRepository driverRepository;
  private final VehicleRepository vehicleRepository;
  private final RouteEventRepository routeEventRepository;
  private final RouteStopResolver stopResolver;

  public IntegrationDataService(
    OrderRepository orderRepository,
    CustomerRepository customerRepository,
    SavedRouteRepository routeRepository,
    DriverRepository driverRepository,
    VehicleRepository vehicleRepository,
    RouteEventRepository routeEventRepository,
    RouteStopResolver stopResolver
  ) {
    this.orderRepository = orderRepository;
    this.customerRepository = customerRepository;
    this.routeRepository = routeRepository;
    this.driverRepository = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.routeEventRepository = routeEventRepository;
    this.stopResolver = stopResolver;
  }

  /** Pedidos, opcionalmente filtrados por status. */
  public List<IntegrationOrderDto> orders(String status) {
    Map<UUID, String> customerNames = customerNames();

    return orderRepository.findAll().stream()
      .filter(order -> status == null || status.equals(order.getStatus()))
      .sorted(Comparator.comparing(Order::getDate).reversed())
      .map(order -> new IntegrationOrderDto(
        order.getId(),
        order.getDate(),
        order.getStatus(),
        customerNames.get(order.getCustomerId()),
        total(order),
        order.getDeliveredAt()
      ))
      .toList();
  }

  /** Rotas, da mais recente para a mais antiga. */
  public List<IntegrationRouteDto> routes() {
    List<SavedRoute> routes = routeRepository.findAllByOrderByCreatedAtDesc();
    if (routes.isEmpty()) {
      return List.of();
    }

    Map<UUID, String> driverNames = driverRepository.findAll().stream()
      .collect(Collectors.toMap(Driver::getId, Driver::getFullName));
    Map<UUID, String> vehiclePlates = vehicleRepository.findAll().stream()
      .collect(Collectors.toMap(Vehicle::getId, Vehicle::getPlate));

    // Progresso de todas as rotas numa consulta só — uma por rota seria N+1 numa lista
    // que um integrador pode pedir inteira.
    Map<UUID, Set<Integer>> deliveredByRoute = routeEventRepository
      .findByRouteIdIn(routes.stream().map(SavedRoute::getId).toList())
      .stream()
      .filter(event -> "entrega".equals(event.getType()) && event.getStopIndex() != null)
      .collect(Collectors.groupingBy(
        RouteEvent::getRouteId,
        Collectors.mapping(RouteEvent::getStopIndex, Collectors.toSet())
      ));

    return routes.stream()
      .map(route -> new IntegrationRouteDto(
        route.getCreatedAt(),
        route.getStatus(),
        route.getDepartureTime(),
        driverNames.get(route.getDriverId()),
        vehiclePlates.get(route.getVehicleId()),
        route.getOrdersCount(),
        stopResolver.countStops(route),
        deliveredByRoute.getOrDefault(route.getId(), Set.of()).size(),
        route.getTotalDistanceKm()
      ))
      .toList();
  }

  /**
   * Entregas concluídas, da mais recente para a mais antiga.
   *
   * <p>Só pedidos com {@code deliveredAt} gravado. Os entregues antes da V19 ficam de
   * fora: sem horário, a linha não responde à pergunta que este endpoint existe para
   * responder, e devolver "entregue em nulo" seria pior do que não devolver.
   */
  public List<IntegrationDeliveryDto> deliveries(Instant since) {
    Map<UUID, String> customerNames = customerNames();

    return orderRepository.findAll().stream()
      .filter(order -> order.getDeliveredAt() != null)
      .filter(order -> since == null || !order.getDeliveredAt().isBefore(since))
      .sorted(Comparator.comparing(Order::getDeliveredAt).reversed())
      .map(order -> new IntegrationDeliveryDto(
        order.getId(),
        customerNames.get(order.getCustomerId()),
        order.getDeliveredAt(),
        total(order)
      ))
      .toList();
  }

  private Map<UUID, String> customerNames() {
    return customerRepository.findAll().stream()
      .collect(Collectors.toMap(Customer::getId, Customer::getName));
  }

  /** Mesma fórmula de {@code calculateOrderTotal} no front: o desconto não leva o frete
   *  junto para baixo de zero. */
  private double total(Order order) {
    double subtotal = order.getItems().stream()
      .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
      .sum();
    return Math.max(0, subtotal - order.getDiscount()) + order.getShippingCost();
  }
}
