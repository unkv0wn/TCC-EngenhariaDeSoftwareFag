package com.routewise.service;

import com.routewise.dto.OrderDto;
import com.routewise.dto.OrderHistoryDto;
import com.routewise.dto.OrderItemDto;
import com.routewise.dto.OrderItemRequestDto;
import com.routewise.dto.OrderRequestDto;
import com.routewise.entity.Order;
import com.routewise.entity.OrderHistoryEntry;
import com.routewise.entity.OrderItem;
import com.routewise.exception.InvalidOrderStatusTransitionException;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements IOrderService {

  /**
   * Transições válidas de status — espelha VALID_STATUS_TRANSITIONS de useOrders.ts no
   * front-end. "Faturado" é um passo real da linha do tempo do pedido, não um flag à parte:
   * isso garante que só existam combinações que fazem sentido (ex: não dá pra estar em_rota
   * sem antes ter passado por faturado, nem faturar duas vezes o mesmo pedido).
   */
  private static final Map<String, List<String>> VALID_STATUS_TRANSITIONS = Map.of(
    "aguardando", List.of("faturado", "cancelado"),
    "faturado", List.of("em_rota", "cancelado"),
    // Uma vez em rota, a mercadoria já saiu — não é mais cancelável por aqui. "faturado" aqui
    // é o "Retorno à empresa" (ver comentário equivalente em useOrders.ts).
    "em_rota", List.of("entregue", "faturado"),
    "entregue", List.of(),
    "cancelado", List.of()
  );

  private final OrderRepository repository;

  public OrderServiceImpl(OrderRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrderDto> findAll() {
    return repository.findAll().stream().map(this::toDto).toList();
  }

  @Override
  public OrderDto create(OrderRequestDto request) {
    Order entity = new Order(UUID.randomUUID());
    applyRequest(entity, request);
    entity.getHistory().add(new OrderHistoryEntry(request.status(), Instant.now()));
    return toDto(repository.save(entity));
  }

  @Override
  public OrderDto update(UUID id, OrderRequestDto request) {
    Order entity = findOrThrow(id);
    boolean statusChanged = !entity.getStatus().equals(request.status());
    applyRequest(entity, request);
    if (statusChanged) {
      entity.getHistory().add(new OrderHistoryEntry(request.status(), Instant.now()));
    }
    return toDto(repository.save(entity));
  }

  @Override
  public void delete(UUID id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Pedido não encontrado: " + id);
    }
    repository.deleteById(id);
  }

  @Override
  public OrderDto changeStatus(UUID id, String status) {
    Order entity = findOrThrow(id);
    if (entity.getStatus().equals(status)) {
      return toDto(entity);
    }
    List<String> allowed = VALID_STATUS_TRANSITIONS.getOrDefault(entity.getStatus(), List.of());
    if (!allowed.contains(status)) {
      throw new InvalidOrderStatusTransitionException(
        "Não é possível mudar de '" + entity.getStatus() + "' para '" + status + "'."
      );
    }
    entity.setStatus(status);
    entity.getHistory().add(new OrderHistoryEntry(status, Instant.now()));
    return toDto(repository.save(entity));
  }

  private void applyRequest(Order entity, OrderRequestDto request) {
    entity.setCustomerId(request.customerId());
    entity.setVehicleId(request.vehicleId());
    entity.setDriverId(request.driverId());
    entity.setPaymentMethodId(request.paymentMethodId());
    entity.setPaymentConditionId(request.paymentConditionId());
    entity.setDate(request.date());
    entity.setStatus(request.status());
    entity.setDiscount(request.discount());
    entity.setShippingCost(request.shippingCost());
    entity.setNotes(request.notes());

    entity.getItems().clear();
    for (OrderItemRequestDto item : request.items()) {
      entity.getItems().add(new OrderItem(item.productId(), item.quantity(), item.unitPrice()));
    }
  }

  private Order findOrThrow(UUID id) {
    return repository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
  }

  private OrderDto toDto(Order entity) {
    List<OrderItemDto> items = entity.getItems().stream()
      .map(item -> new OrderItemDto(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
      .toList();
    List<OrderHistoryDto> history = entity.getHistory().stream()
      .map(entry -> new OrderHistoryDto(entry.getStatus(), entry.getChangedAt()))
      .toList();

    return new OrderDto(
      entity.getId(),
      entity.getCustomerId(),
      entity.getVehicleId(),
      entity.getDriverId(),
      entity.getPaymentMethodId(),
      entity.getPaymentConditionId(),
      entity.getDate(),
      entity.getStatus(),
      items,
      entity.getDiscount(),
      entity.getShippingCost(),
      entity.getNotes(),
      history
    );
  }
}
