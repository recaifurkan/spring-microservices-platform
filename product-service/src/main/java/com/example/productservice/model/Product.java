package com.example.productservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private Integer stock = 0;
    private String category;
    private String brand;
    @Column(name = "image_url") private String imageUrl;
    @Column(nullable = false) private Float rating = 4.0f;
    @Column(name = "review_count", nullable = false) private Integer reviewCount = 0;
    @Column(name = "discount_percent", nullable = false) private Integer discountPercent = 0;
    @Column(nullable = false) private Boolean featured = false;
    @Column(name = "created_by") private String createdBy;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt = LocalDateTime.now();

    /** Orijinal fiyat (indirim uygulanmamış) */
    public BigDecimal getOriginalPrice() {
        if (discountPercent == null || discountPercent == 0) return price;
        return price.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(100 - discountPercent), 2, java.math.RoundingMode.HALF_UP);
    }
}
