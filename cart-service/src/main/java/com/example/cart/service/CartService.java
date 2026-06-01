package com.example.cart.service;

import com.example.cart.client.ProductServiceClient;
import com.example.cart.dto.ProductResponse;
import com.example.cart.model.CartItem;
import com.example.cart.repository.CartItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional
public class CartService {

    private final CartItemRepository repo;
    private final ProductServiceClient productClient;

    public CartService(CartItemRepository repo, ProductServiceClient productClient) {
        this.repo = repo; this.productClient = productClient;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCart(String userId) { return repo.findByUserId(userId); }

    @Transactional(readOnly = true)
    public long getItemCount(String userId) { return repo.countByUserId(userId); }

    public CartItem addItem(String userId, Long productId, Integer quantity) {
        ProductResponse product;
        try { product = productClient.getProduct(productId); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ürün servisi erişilemiyor"); }

        int stock = product.stock() != null ? product.stock() : 0;
        if (stock < quantity)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yetersiz stok: " + stock);

        CartItem item = repo.findByUserIdAndProductId(userId, productId)
                .orElse(new CartItem());
        item.setUserId(userId);
        item.setProductId(productId);
        item.setProductName(product.name() != null ? product.name() : "Ürün #" + productId);
        item.setProductPrice(product.price() != null ? product.price() : BigDecimal.ZERO);

        int newQty = (item.getQuantity() != null && item.getId() != null ? item.getQuantity() : 0) + quantity;
        if (newQty > stock) throw new ResponseStatusException(HttpStatus.CONFLICT, "Stok yetersiz");
        item.setQuantity(newQty);
        if (item.getId() == null) item.setAddedAt(LocalDateTime.now());

        return repo.save(item);
    }

    public CartItem updateQuantity(String userId, Long productId, Integer quantity) {
        CartItem item = repo.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sepette bu ürün yok"));
        if (quantity <= 0) { repo.delete(item); return item; }
        item.setQuantity(quantity);
        return repo.save(item);
    }

    public void removeItem(String userId, Long productId) {
        repo.deleteByUserIdAndProductId(userId, productId);
    }

    public void clearCart(String userId) { repo.deleteByUserId(userId); }

    @Transactional(readOnly = true)
    public BigDecimal getTotal(String userId) {
        return repo.findByUserId(userId).stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
