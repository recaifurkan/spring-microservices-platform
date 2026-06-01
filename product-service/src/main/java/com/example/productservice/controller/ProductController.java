package com.example.productservice.controller;
import com.example.productservice.dto.ProductSuggestionResponse;
import com.example.productservice.model.Product;
import com.example.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Service", description = "Ürün kataloğu yönetimi")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    // ── Public endpoints (no auth required) ──────────────────────────────────

    @Operation(summary = "Tüm ürünleri listele — filtre/sıralama destekli (public)")
    @GetMapping
    public List<Product> listAll(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String sort,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice) {
        if (category == null && search == null && minPrice == null && maxPrice == null && sort == null)
            return service.findAll();
        return service.filter(category, minPrice, maxPrice, search, sort);
    }

    @Operation(summary = "Ürün ara (public)")
    @GetMapping("/search")
    public List<Product> search(@RequestParam(required = false, defaultValue = "") String q) {
        return service.search(q);
    }

    @Operation(summary = "Öne çıkan ürünler (public)")
    @GetMapping("/featured")
    public List<Product> featured() { return service.findFeatured(); }

    @Operation(summary = "En yüksek puanlı ürünler (public)")
    @GetMapping("/top-rated")
    public List<Product> topRated(@RequestParam(defaultValue = "8") int limit) {
        return service.findTopRated(limit);
    }

    @Operation(summary = "Yeni eklenenler (public)")
    @GetMapping("/newest")
    public List<Product> newest(@RequestParam(defaultValue = "8") int limit) {
        return service.findNewest(limit);
    }

    @Operation(summary = "Kategoriler listesi (public)")
    @GetMapping("/categories")
    public List<String> categories() { return service.findCategories(); }

    @Operation(summary = "Ürün detayı (public)")
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "İlgili / önerilen ürünler (public)")
    @GetMapping("/{id}/related")
    public List<Product> related(@PathVariable Long id,
                                 @RequestParam(defaultValue = "4") int limit) {
        return service.findRelated(id, limit);
    }

    @Operation(summary = "Arama önerileri (public)")
    @GetMapping("/suggestions")
    public List<ProductSuggestionResponse> suggestions(@RequestParam String q) {
        return service.search(q).stream()
                .limit(8)
                .map(p -> new ProductSuggestionResponse(
                        p.getId(), p.getName(),
                        p.getCategory() != null ? p.getCategory() : ""))
                .toList();
    }

    // ── Authenticated endpoints ───────────────────────────────────────────────

    @Operation(summary = "Ürün oluştur (catalog:write — MANAGER veya ADMIN)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_catalog:write') and hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Product> create(@RequestBody Product product, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(product, jwt.getSubject()));
    }

    @Operation(summary = "Ürün güncelle", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_catalog:write') and hasAnyRole('ADMIN','MANAGER')")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        return service.update(id, product);
    }

    @Operation(summary = "Ürün sil", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_catalog:write') and hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Stok azalt (servisler arası — catalog:write)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/stock/decrease")
    @PreAuthorize("hasAuthority('SCOPE_catalog:write')")
    public Product decreaseStock(@PathVariable Long id, @RequestParam int quantity) {
        return service.decreaseStock(id, quantity);
    }

    @Operation(summary = "Stok artır (servisler arası — sipariş iptali için catalog:write)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/{id}/stock/increase")
    @PreAuthorize("hasAuthority('SCOPE_catalog:write')")
    public Product increaseStock(@PathVariable Long id, @RequestParam int quantity) {
        return service.increaseStock(id, quantity);
    }
}
