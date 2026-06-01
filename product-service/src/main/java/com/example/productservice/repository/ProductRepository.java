package com.example.productservice.repository;
import com.example.productservice.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);
    List<Product> findByCreatedBy(String createdBy);
    List<Product> findByStockGreaterThan(int minStock);

    // Search (name, description, category, brand)
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(p.description,'')) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(p.category,'')) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(p.brand,'')) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Product> search(@Param("q") String q);

    // Combined category + search query
    @Query("SELECT p FROM Product p WHERE p.category = :cat AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(p.description,'')) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Product> searchInCategory(@Param("cat") String category, @Param("q") String q);

    // Distinct categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategories();

    // Price range + category
    @Query("SELECT p FROM Product p WHERE " +
           "(:cat IS NULL OR p.category = :cat) AND " +
           "(:minP IS NULL OR p.price >= :minP) AND " +
           "(:maxP IS NULL OR p.price <= :maxP) AND " +
           "(:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           " LOWER(COALESCE(p.brand,'')) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Product> filter(@Param("cat") String category,
                         @Param("minP") BigDecimal minPrice,
                         @Param("maxP") BigDecimal maxPrice,
                         @Param("q") String search);

    // Featured products
    List<Product> findByFeaturedTrueAndStockGreaterThanOrderByRatingDesc(int minStock);

    // Related products (recommendations)
    List<Product> findByCategoryAndIdNotAndStockGreaterThanOrderByRatingDesc(
            String category, Long excludeId, int minStock, Pageable pageable);

    // Highest-rated products
    @Query("SELECT p FROM Product p WHERE p.stock > 0 ORDER BY p.rating DESC, p.reviewCount DESC")
    List<Product> findTopRated(Pageable pageable);

    // Newly added products
    @Query("SELECT p FROM Product p WHERE p.stock > 0 ORDER BY p.createdAt DESC")
    List<Product> findNewest(Pageable pageable);

    // Category + stock filter
    List<Product> findByCategoryAndStockGreaterThan(String category, int minStock);
}
