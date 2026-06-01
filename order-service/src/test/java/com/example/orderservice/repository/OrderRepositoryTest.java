package com.example.orderservice.repository;

import com.example.orderservice.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
class OrderRepositoryTest {

    @Autowired OrderRepository repo;

    private Order buildOrder(String userId, Long productId, Order.OrderStatus status) {
        Order o = new Order();
        o.setUserId(userId);
        o.setProductId(productId);
        o.setQuantity(1);
        o.setTotalPrice(BigDecimal.TEN);
        o.setStatus(status);
        return o;
    }

    @Test
    void findByUserId() {
        repo.save(buildOrder("u1", 1L, Order.OrderStatus.PENDING));
        repo.save(buildOrder("u1", 2L, Order.OrderStatus.CONFIRMED));
        repo.save(buildOrder("u2", 1L, Order.OrderStatus.PENDING));

        List<Order> result = repo.findByUserId("u1");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> "u1".equals(o.getUserId()));
    }

    @Test
    void findByStatus() {
        repo.save(buildOrder("u1", 1L, Order.OrderStatus.PENDING));
        repo.save(buildOrder("u2", 2L, Order.OrderStatus.CANCELLED));

        List<Order> pending = repo.findByStatus(Order.OrderStatus.PENDING);
        assertThat(pending).isNotEmpty();
        assertThat(pending).allMatch(o -> o.getStatus() == Order.OrderStatus.PENDING);
    }

    @Test
    void findByProductId() {
        repo.save(buildOrder("u1", 42L, Order.OrderStatus.PENDING));
        List<Order> result = repo.findByProductId(42L);
        assertThat(result).isNotEmpty();
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_orderedCorrectly() {
        repo.save(buildOrder("u3", 1L, Order.OrderStatus.PENDING));
        repo.save(buildOrder("u3", 2L, Order.OrderStatus.CONFIRMED));
        List<Order> result = repo.findByUserIdOrderByCreatedAtDesc("u3");
        assertThat(result).hasSize(2);
    }
}

