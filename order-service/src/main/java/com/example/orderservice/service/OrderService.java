package com.example.orderservice.service;

import com.example.orderservice.client.NotificationServiceClient;
import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.client.ProductStockClient;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.NotificationRequest;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service @Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repo;
    private final ProductServiceClient productClient;
    private final ProductStockClient stockClient;
    private final NotificationServiceClient notificationClient;

    public OrderService(OrderRepository repo, ProductServiceClient productClient,
                        ProductStockClient stockClient,
                        NotificationServiceClient notificationClient) {
        this.repo = repo; this.productClient = productClient;
        this.stockClient = stockClient;
        this.notificationClient = notificationClient;
    }

    @Transactional(readOnly = true)
    public List<Order> findByUserId(String userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return repo.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Sipariş bulunamadı: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() { return repo.findAll(); }

    public Order createOrder(String userId, Long productId, Integer quantity) {
        ProductResponse product;
        try { product = productClient.getProduct(productId); }
        catch (Exception e) {
            log.warn("Product service unavailable: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Ürün servisi erişilemiyor");
        }

        int stock = product.stock() != null ? product.stock() : 0;
        if (stock < quantity) throw new ResponseStatusException(HttpStatus.CONFLICT, "Yetersiz stok");

        BigDecimal price = product.price() != null ? product.price() : BigDecimal.ZERO;
        String productName = product.name() != null ? product.name() : "Ürün #" + productId;

        Order order = new Order();
        order.setUserId(userId); order.setProductId(productId); order.setProductName(productName);
        order.setQuantity(quantity);
        order.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now()); order.setUpdatedAt(LocalDateTime.now());

        Order saved = repo.save(order);

        log.info("Sipariş oluşturuldu: orderId={} userId={} productId={} productName={} qty={} total={}",
                saved.getId(), userId, productId, productName, quantity, saved.getTotalPrice());

        // Service-account token ile stok azalt (catalog:write scope gerektirir)
        try { stockClient.decreaseStock(productId, quantity); }
        catch (Exception e) {
            log.error("Stok azaltma başarısız! productId={} quantity={} — {}",
                    productId, quantity, e.getMessage());
        }

        sendNotification(userId, "ORDER_CREATED",
                "Siparişiniz oluşturuldu! #" + saved.getId() + " x" + quantity);

        return saved;
    }

    public List<Order> createOrdersFromCart(String userId, List<CreateOrderRequest> items) {
        if (items == null || items.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sepet boş");
        List<Order> orders = new ArrayList<>();
        for (CreateOrderRequest item : items) {
            orders.add(createOrder(userId, item.productId(), item.quantity()));
        }
        return orders;
    }

    public Order updateStatus(Long id, Order.OrderStatus status) {
        Order order = findById(id);
        order.setStatus(status); order.setUpdatedAt(LocalDateTime.now());
        Order saved = repo.save(order);
        log.info("Order status updated: orderId={} newStatus={}", id, status);
        sendNotification(order.getUserId(), "ORDER_STATUS_CHANGED",
                "Order #" + id + " status updated: " + status.name());
        return saved;
    }

    public Order cancelOrder(Long id, String userId) {
        Order order = findById(id);
        if (!order.getUserId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This order does not belong to you");
        if (order.getStatus() != Order.OrderStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending orders can be cancelled");
        Order cancelled = updateStatus(id, Order.OrderStatus.CANCELLED);
        log.info("Order cancelled: orderId={} userId={}", id, userId);
        // Restore stock when the order is cancelled
        try { stockClient.increaseStock(order.getProductId(), order.getQuantity()); }
        catch (Exception e) {
            log.error("Sipariş iptali sonrası stok iadesi başarısız! orderId={} — {}", id, e.getMessage());
        }
        return cancelled;
    }

    private void sendNotification(String userId, String type, String message) {
        try { notificationClient.send(new NotificationRequest(userId, type, message)); }
        catch (Exception e) { log.warn("Notification send failed: {}", e.getMessage()); }
    }
}
