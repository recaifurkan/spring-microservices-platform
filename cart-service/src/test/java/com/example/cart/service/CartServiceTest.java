package com.example.cart.service;

import com.example.cart.client.ProductServiceClient;
import com.example.cart.dto.ProductResponse;
import com.example.cart.model.CartItem;
import com.example.cart.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartItemRepository repo;
    @Mock ProductServiceClient productClient;
    @InjectMocks CartService service;

    private CartItem item;

    @BeforeEach
    void setUp() {
        item = new CartItem();
        item.setId(1L);
        item.setUserId("user-1");
        item.setProductId(10L);
        item.setProductName("Phone");
        item.setProductPrice(new BigDecimal("100.00"));
        item.setQuantity(2);
    }

    @Test
    void givenItemsWhenCartRequestedThenReturnsItems() {
        when(repo.findByUserId("user-1")).thenReturn(List.of(item));

        assertThat(service.getCart("user-1")).hasSize(1);
    }

    @Test
    void givenItemsWhenCountRequestedThenReturnsCount() {
        when(repo.countByUserId("user-1")).thenReturn(3L);

        assertThat(service.getItemCount("user-1")).isEqualTo(3L);
    }

    @Test
    void givenEnoughStockWhenAddingNewItemThenSavesNewCartItem() {
        when(productClient.getProduct(10L)).thenReturn(new ProductResponse(10L, "Phone", new BigDecimal("100.00"), 5, "Electronics", null));
        when(repo.findByUserIdAndProductId("user-1", 10L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = service.addItem("user-1", 10L, 2);

        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getSubtotal()).isEqualByComparingTo("200.00");
        verify(repo).save(any());
    }

    @Test
    void givenExistingItemWhenAddingThenIncreasesQuantity() {
        item.setQuantity(2);
        when(productClient.getProduct(10L)).thenReturn(new ProductResponse(10L, "Phone", new BigDecimal("100.00"), 10, "Electronics", null));
        when(repo.findByUserIdAndProductId("user-1", 10L)).thenReturn(Optional.of(item));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartItem result = service.addItem("user-1", 10L, 3);

        assertThat(result.getQuantity()).isEqualTo(5);
    }

    @Test
    void givenInsufficientStockWhenAddingThenThrowsConflict() {
        when(productClient.getProduct(10L)).thenReturn(new ProductResponse(10L, "Phone", new BigDecimal("100.00"), 1, "Electronics", null));

        assertThatThrownBy(() -> service.addItem("user-1", 10L, 2))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void givenProductClientFailureWhenAddingThenThrowsServiceUnavailable() {
        when(productClient.getProduct(10L)).thenThrow(new RuntimeException("down"));

        assertThatThrownBy(() -> service.addItem("user-1", 10L, 1))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void givenItemWhenUpdatingToZeroThenDeletesItem() {
        when(repo.findByUserIdAndProductId("user-1", 10L)).thenReturn(Optional.of(item));

        CartItem result = service.updateQuantity("user-1", 10L, 0);

        assertThat(result).isEqualTo(item);
        verify(repo).delete(item);
    }

    @Test
    void givenMissingItemWhenUpdatingThenThrowsNotFound() {
        when(repo.findByUserIdAndProductId("user-1", 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateQuantity("user-1", 10L, 1))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenUserAndProductWhenRemovingThenDeletesByCompositeKey() {
        service.removeItem("user-1", 10L);
        verify(repo).deleteByUserIdAndProductId("user-1", 10L);
    }

    @Test
    void givenUserWhenClearingThenDeletesAllUserItems() {
        service.clearCart("user-1");
        verify(repo).deleteByUserId("user-1");
    }

    @Test
    void givenItemsWhenTotalRequestedThenSumsSubtotals() {
        when(repo.findByUserId("user-1")).thenReturn(List.of(item));
        assertThat(service.getTotal("user-1")).isEqualByComparingTo("200.00");
    }
}

