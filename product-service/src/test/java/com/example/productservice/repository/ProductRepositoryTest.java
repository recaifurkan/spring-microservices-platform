package com.example.productservice.repository;
import com.example.productservice.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {"spring.flyway.enabled=true","spring.flyway.locations=classpath:db/migration"})
class ProductRepositoryTest {
    @Autowired ProductRepository repo;

    @Test
    void findByCategory() {
        // V2 migration inserts 'Elektronik' products
        List<Product> result = repo.findByCategory("Elektronik");
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(p -> "Elektronik".equals(p.getCategory()));
    }

    @Test
    void findByCreatedBy() {
        List<Product> result = repo.findByCreatedBy("system");
        assertThat(result).isNotEmpty();
    }

    @Test
    void findByStockGreaterThan() {
        List<Product> result = repo.findByStockGreaterThan(0);
        assertThat(result).isNotEmpty();
    }

    @Test
    void saveAndFind() {
        Product p = new Product();
        p.setName("New Product");
        p.setPrice(BigDecimal.TEN);
        p.setStock(5);
        repo.save(p);
        assertThat(repo.findAll()).anyMatch(x -> "New Product".equals(x.getName()));
    }
}

