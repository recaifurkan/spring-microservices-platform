package com.example.frontend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Product-service'ten kategori listesini çeker ve bellek önbelleğinde tutar.
 * <p>
 * Her 5 dakikada bir otomatik yenileme yapar. Servis kapalıysa son başarılı
 * listeye ya da boş listeye düşer — uygulama asla patlamaz.
 */
@Service
public class CategoryCacheService {

    private static final Logger log = LoggerFactory.getLogger(CategoryCacheService.class);

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    private final RestTemplate restTemplate;
    private final AtomicReference<List<String>> cache = new AtomicReference<>(List.of());

    public CategoryCacheService(@Qualifier("publicRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Uygulama ayağa kalkarken ilk yüklemeyi yap. */
    @jakarta.annotation.PostConstruct
    public void init() {
        refresh();
    }

    /** Her 5 dakikada bir yenile. */
    @Scheduled(fixedDelay = 5 * 60 * 1_000)
    public void refresh() {
        try {
            @SuppressWarnings("unchecked")
            List<String> fresh = restTemplate.getForObject(
                    gatewayUrl + "/api/products/categories", List.class);
            if (fresh != null && !fresh.isEmpty()) {
                cache.set(fresh);
                log.info("Kategori önbelleği güncellendi: {} kategori", fresh.size());
            }
        } catch (Exception e) {
            log.warn("Kategori listesi yüklenemedi, önbellek korunuyor: {}", e.getMessage());
        }
    }

    /** Güncel kategori listesini döner. Hiç yüklenemezse boş liste. */
    public List<String> getCategories() {
        return cache.get();
    }
}
