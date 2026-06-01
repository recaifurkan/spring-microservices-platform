package com.example.frontend.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Her controller metoduna otomatik olarak şu model attribute'larını ekler:
 * <ul>
 *   <li>{@code globalCategories} — DB'den gelen canlı kategori listesi</li>
 * </ul>
 * Thymeleaf fragment'larında (navbar catbar gibi) {@code ${globalCategories}}
 * ile erişilebilir — controller'a ayrı satır yazmak gerekmez.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CategoryCacheService categoryCacheService;

    public GlobalModelAdvice(CategoryCacheService categoryCacheService) {
        this.categoryCacheService = categoryCacheService;
    }

    @ModelAttribute("globalCategories")
    public List<String> globalCategories() {
        return categoryCacheService.getCategories();
    }
}

