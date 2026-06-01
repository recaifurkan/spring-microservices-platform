package com.example.productservice.service;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service @Transactional
public class ProductService {
    private final ProductRepository repo;
    public ProductService(ProductRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<Product> findAll() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategory(String category) {
        return repo.findByCategoryAndStockGreaterThan(category, -1);
    }

    @Transactional(readOnly = true)
    public List<String> findCategories() { return repo.findDistinctCategories(); }

    @Transactional(readOnly = true)
    public List<Product> findFeatured() {
        return repo.findByFeaturedTrueAndStockGreaterThanOrderByRatingDesc(0);
    }

    @Transactional(readOnly = true)
    public List<Product> findRelated(Long productId, int limit) {
        Product p = findById(productId);
        if (p.getCategory() == null) return repo.findTopRated(PageRequest.of(0, limit));
        List<Product> related = repo.findByCategoryAndIdNotAndStockGreaterThanOrderByRatingDesc(
                p.getCategory(), productId, 0, PageRequest.of(0, limit));
        if (related.size() < limit) {
            related = repo.findTopRated(PageRequest.of(0, limit)).stream()
                    .filter(r -> !r.getId().equals(productId)).limit(limit).toList();
        }
        return related;
    }

    @Transactional(readOnly = true)
    public List<Product> search(String q) {
        if (q == null || q.isBlank()) return repo.findTopRated(PageRequest.of(0, 20));
        return repo.search(q.trim());
    }

    @Transactional(readOnly = true)
    public List<Product> filter(String category, BigDecimal minPrice, BigDecimal maxPrice,
                                 String search, String sort) {
        List<Product> results = repo.filter(
                (category != null && !category.isBlank()) ? category : null,
                minPrice, maxPrice,
                (search != null && !search.isBlank()) ? search : null);
        if (sort == null) return results;
        return switch (sort) {
            case "price_asc"  -> results.stream().sorted(Comparator.comparing(Product::getPrice)).toList();
            case "price_desc" -> results.stream().sorted(Comparator.comparing(Product::getPrice).reversed()).toList();
            case "rating"     -> results.stream().sorted(Comparator.comparing(Product::getRating).reversed()).toList();
            case "newest"     -> results.stream().sorted(Comparator.comparing(Product::getCreatedAt).reversed()).toList();
            case "name"       -> results.stream().sorted(Comparator.comparing(Product::getName)).toList();
            default           -> results;
        };
    }

    @Transactional(readOnly = true)
    public List<Product> findTopRated(int n) { return repo.findTopRated(PageRequest.of(0, n)); }

    @Transactional(readOnly = true)
    public List<Product> findNewest(int n) { return repo.findNewest(PageRequest.of(0, n)); }

    public Product create(Product product, String userId) {
        product.setCreatedBy(userId);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return repo.save(product);
    }

    public Product update(Long id, Product updated) {
        Product existing = findById(id);
        if (updated.getName()            != null) existing.setName(updated.getName());
        if (updated.getDescription()     != null) existing.setDescription(updated.getDescription());
        if (updated.getPrice()           != null) existing.setPrice(updated.getPrice());
        if (updated.getStock()           != null) existing.setStock(updated.getStock());
        if (updated.getCategory()        != null) existing.setCategory(updated.getCategory());
        if (updated.getBrand()           != null) existing.setBrand(updated.getBrand());
        if (updated.getImageUrl()        != null) existing.setImageUrl(updated.getImageUrl());
        if (updated.getRating()          != null) existing.setRating(updated.getRating());
        if (updated.getReviewCount()     != null) existing.setReviewCount(updated.getReviewCount());
        if (updated.getDiscountPercent() != null) existing.setDiscountPercent(updated.getDiscountPercent());
        if (updated.getFeatured()        != null) existing.setFeatured(updated.getFeatured());
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    public void delete(Long id) { repo.delete(findById(id)); }

    public Product decreaseStock(Long id, int quantity) {
        Product p = findById(id);
        if (p.getStock() < quantity) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        p.setStock(p.getStock() - quantity);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    public Product increaseStock(Long id, int quantity) {
        Product p = findById(id);
        p.setStock(p.getStock() + quantity);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }
}
