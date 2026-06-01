package com.example.productservice.service;

import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository repo;
    @InjectMocks ProductService service;

    private Product sample;

    @BeforeEach
    void setUp() {
        sample = new Product();
        sample.setName("Test Product");
        sample.setPrice(new BigDecimal("99.99"));
        sample.setStock(10);
        sample.setCategory("Test");
    }

    @Test
    void findAll_returnsList() {
        when(repo.findAll()).thenReturn(List.of(sample));
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void findById_found() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThat(service.findById(1L).getName()).isEqualTo("Test Product");
    }

    @Test
    void findById_notFound_throws() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void create_savesProduct() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Product result = service.create(sample, "creator-user");
        assertThat(result.getCreatedBy()).isEqualTo("creator-user");
        verify(repo).save(any());
    }

    @Test
    void update_updatesFields() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Product updated = new Product();
        updated.setName("Updated");
        updated.setPrice(new BigDecimal("149.99"));
        Product result = service.update(1L, updated);
        assertThat(result.getName()).isEqualTo("Updated");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("149.99"));
    }

    @Test
    void delete_callsRepository() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        service.delete(1L);
        verify(repo).delete(sample);
    }

    @Test
    void decreaseStock_sufficientStock_decreases() {
        sample.setStock(20);
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Product result = service.decreaseStock(1L, 5);
        assertThat(result.getStock()).isEqualTo(15);
    }

    @Test
    void decreaseStock_insufficientStock_throws() {
        sample.setStock(2);
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> service.decreaseStock(1L, 10)).isInstanceOf(ResponseStatusException.class);
    }
}

