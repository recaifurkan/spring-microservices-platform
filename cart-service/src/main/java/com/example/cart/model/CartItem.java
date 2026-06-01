package com.example.cart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter @Setter @NoArgsConstructor
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "product_name") private String productName;
    @Column(name = "product_price", precision = 12, scale = 2) private BigDecimal productPrice;
    @Column(nullable = false) private Integer quantity = 1;
    @Column(name = "added_at") private LocalDateTime addedAt = LocalDateTime.now();

    public BigDecimal getSubtotal() {
        if (productPrice == null) return BigDecimal.ZERO;
        return productPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
