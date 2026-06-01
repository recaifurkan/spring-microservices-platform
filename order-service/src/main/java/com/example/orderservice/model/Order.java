package com.example.orderservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "product_name") private String productName;
    @Column(nullable = false) private Integer quantity;
    @Column(name = "total_price", precision = 12, scale = 2) private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING) private OrderStatus status = OrderStatus.PENDING;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt = LocalDateTime.now();

    public enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
}
