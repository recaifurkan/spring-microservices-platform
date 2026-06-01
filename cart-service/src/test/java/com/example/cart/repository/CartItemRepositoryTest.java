package com.example.cart.repository;

import com.example.cart.model.CartItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CartItemRepositoryTest {

    @Autowired CartItemRepository repository;

    @Test
    void givenPersistedCartItemWhenQueryingThenFindsByUserAndCountsCorrectly() {
        CartItem item = new CartItem();
        item.setUserId("user-1");
        item.setProductId(10L);
        item.setProductName("Phone");
        item.setProductPrice(new BigDecimal("50.00"));
        item.setQuantity(2);
        repository.save(item);

        assertThat(repository.findByUserId("user-1")).hasSize(1);
        assertThat(repository.findByUserIdAndProductId("user-1", 10L)).isPresent();
        assertThat(repository.countByUserId("user-1")).isEqualTo(1);
        assertThat(repository.findByUserId("user-1").get(0).getSubtotal()).isEqualByComparingTo("100.00");
    }
}

