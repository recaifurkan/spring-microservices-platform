package com.example.orderservice.service;

import com.example.orderservice.client.NotificationServiceClient;
import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.client.ProductStockClient;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository repo;
    @Mock ProductServiceClient productClient;
    @Mock ProductStockClient stockClient;
    @Mock NotificationServiceClient notificationClient;
    @InjectMocks OrderService service;

    private Order sample;

    @BeforeEach
    void setUp() {
        sample = new Order();
        sample.setUserId("user-1");
        sample.setProductId(1L);
        sample.setQuantity(2);
        sample.setTotalPrice(new BigDecimal("199.98"));
        sample.setStatus(Order.OrderStatus.PENDING);
    }

    @Test
    void findByUserId_returnsList() {
        when(repo.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(sample));
        assertThat(service.findByUserId("user-1")).hasSize(1);
    }

    @Test
    void findById_found() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThat(service.findById(1L).getUserId()).isEqualTo("user-1");
    }

    @Test
    void findById_notFound_throws() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createOrder_success() {
        ProductResponse product = new ProductResponse(1L, "Test", new BigDecimal("99.99"), 10, null);
        ProductResponse decreased = new ProductResponse(1L, "Test", new BigDecimal("99.99"), 8, null);
        when(productClient.getProduct(1L)).thenReturn(product);
        when(stockClient.decreaseStock(eq(1L), eq(2))).thenReturn(decreased);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.createOrder("user-1", 1L, 2);
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("199.98"));
        verify(stockClient).decreaseStock(1L, 2);
    }

    @Test
    void createOrder_insufficientStock_throws() {
        when(productClient.getProduct(1L)).thenReturn(new ProductResponse(1L, "Test", new BigDecimal("99.99"), 1, null));
        assertThatThrownBy(() -> service.createOrder("user-1", 1L, 5))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createOrder_productServiceDown_throws() {
        when(productClient.getProduct(1L)).thenThrow(new RuntimeException("Connection refused"));
        assertThatThrownBy(() -> service.createOrder("user-1", 1L, 1))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateStatus_updates() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Order result = service.updateStatus(1L, Order.OrderStatus.CONFIRMED);
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    void cancelOrder_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Order result = service.cancelOrder(1L, "user-1");
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(stockClient).increaseStock(sample.getProductId(), sample.getQuantity());
    }

    @Test
    void cancelOrder_wrongUser_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> service.cancelOrder(1L, "other-user"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void cancelOrder_notPending_throws() {
        sample.setStatus(Order.OrderStatus.CONFIRMED);
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> service.cancelOrder(1L, "user-1"))
            .isInstanceOf(ResponseStatusException.class);
    }
}
