package com.example.orderservice.repository;
import com.example.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
    List<Order> findByProductId(Long productId);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
}

