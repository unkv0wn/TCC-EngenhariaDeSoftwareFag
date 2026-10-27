package com.routewise.repository;

import com.routewise.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  /** Entregues a partir de um instante — pela data real da entrega, não pela do pedido. */
  List<Order> findByDeliveredAtGreaterThanEqual(Instant from);

  long countByStatus(String status);
}
