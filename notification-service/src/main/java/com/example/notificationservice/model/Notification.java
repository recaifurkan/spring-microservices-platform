package com.example.notificationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(nullable = false) private String type;
    @Column(nullable = false, length = 1000) private String message;
    @Column(name = "is_read", nullable = false) private boolean read = false;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
