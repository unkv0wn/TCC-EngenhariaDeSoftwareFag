package com.routewise.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

/** Item de um pedido — valor embutido (sem id próprio), vive dentro de Order.items. */
@Embeddable
public class OrderItem {

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private double unitPrice;

  protected OrderItem() {
    // JPA
  }

  public OrderItem(UUID productId, int quantity, double unitPrice) {
    this.productId = productId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
  }

  public UUID getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public double getUnitPrice() {
    return unitPrice;
  }
}
