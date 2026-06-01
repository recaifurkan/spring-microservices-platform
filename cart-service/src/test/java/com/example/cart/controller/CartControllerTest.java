package com.example.cart.controller;

import com.example.cart.dto.AddItemRequest;
import com.example.cart.dto.CartResponse;
import com.example.cart.dto.UpdateQuantityRequest;
import com.example.cart.model.CartItem;
import com.example.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock CartService service;
    @InjectMocks CartController controller;

    private Jwt jwt() {
        return Jwt.withTokenValue("token").header("alg", "none").subject("user-1").build();
    }

    @Test
    void givenJwtWhenGettingCartThenReturnsSummary() {
        CartItem item = new CartItem();
        item.setQuantity(1);
        when(service.getCart("user-1")).thenReturn(List.of(item));
        when(service.getTotal("user-1")).thenReturn(new BigDecimal("10.00"));

        CartResponse response = controller.getCart(jwt());

        assertThat(response.itemCount()).isEqualTo(1);
        assertThat(response.total()).isEqualByComparingTo("10.00");
    }

    @Test
    void givenJwtWhenGettingCountThenReturnsCountResponse() {
        when(service.getItemCount("user-1")).thenReturn(4L);
        assertThat(controller.getCount(jwt()).count()).isEqualTo(4L);
    }

    @Test
    void givenNoQuantityWhenAddingItemThenDefaultsToOne() {
        CartItem item = new CartItem();
        item.setQuantity(1);
        when(service.addItem("user-1", 10L, 1)).thenReturn(item);

        assertThat(controller.addItem(new AddItemRequest(10L, null), jwt()).getQuantity()).isEqualTo(1);
    }

    @Test
    void givenQuantityWhenUpdatingThenDelegatesToService() {
        CartItem item = new CartItem();
        item.setQuantity(3);
        when(service.updateQuantity("user-1", 10L, 3)).thenReturn(item);

        ResponseEntity<CartItem> response = controller.updateItem(10L, new UpdateQuantityRequest(3), jwt());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getQuantity()).isEqualTo(3);
    }

    @Test
    void givenJwtWhenRemovingThenReturnsNoContent() {
        doNothing().when(service).removeItem("user-1", 10L);
        assertThat(controller.removeItem(10L, jwt()).getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void givenJwtWhenClearingThenReturnsNoContent() {
        doNothing().when(service).clearCart("user-1");
        assertThat(controller.clearCart(jwt()).getStatusCode().value()).isEqualTo(204);
    }
}

