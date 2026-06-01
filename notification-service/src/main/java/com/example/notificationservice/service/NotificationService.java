package com.example.notificationservice.service;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service @Transactional
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<Notification> getByUserId(String userId) { return repo.findByUserIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public long countUnread(String userId) { return repo.countByUserIdAndRead(userId, false); }

    public Notification send(String userId, String type, String message) {
        log.info("[NOTIFICATION] → userId={} type={} message={}", userId, type, message);
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setMessage(message);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return repo.save(n);
    }

    public Notification markRead(Long id, String userId) {
        Notification n = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: " + id));
        if (!n.getUserId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This notification does not belong to you");
        n.setRead(true);
        return repo.save(n);
    }

    public void markAllRead(String userId) {
        repo.findByUserIdAndRead(userId, false).forEach(n -> { n.setRead(true); repo.save(n); });
    }
}

