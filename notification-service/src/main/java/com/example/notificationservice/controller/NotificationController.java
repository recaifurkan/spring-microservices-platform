package com.example.notificationservice.controller;

import com.example.notificationservice.dto.SendNotificationRequest;
import com.example.notificationservice.dto.UnreadCountResponse;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/notifications")
@Tag(name = "Notification Service", description = "Notification management")
public class NotificationController {

    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @Operation(summary = "List my notifications", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public List<Notification> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        return service.getByUserId(jwt.getSubject());
    }

    @Operation(summary = "Unread notification count", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCountResponse(service.countUnread(jwt.getSubject()));
    }

    @Operation(summary = "Send notification (inter-service)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/send")
    public ResponseEntity<Notification> send(@RequestBody SendNotificationRequest req) {
        Notification n = service.send(req.userId(), req.type(), req.message());
        return ResponseEntity.ok(n);
    }

    @Operation(summary = "Mark notification as read", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/read")
    public Notification markRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.markRead(id, jwt.getSubject());
    }

    @Operation(summary = "Mark all as read", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
