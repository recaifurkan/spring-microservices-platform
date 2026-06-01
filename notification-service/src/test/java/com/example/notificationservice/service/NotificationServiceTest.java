package com.example.notificationservice.service;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repo;
    @InjectMocks NotificationService service;

    private Notification sample;

    @BeforeEach
    void setUp() {
        sample = new Notification();
        sample.setUserId("user-1");
        sample.setType("ORDER_CONFIRMED");
        sample.setMessage("Siparişiniz onaylandı.");
        sample.setRead(false);
    }

    @Test
    void getByUserId_returnsList() {
        when(repo.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(sample));
        assertThat(service.getByUserId("user-1")).hasSize(1);
    }

    @Test
    void countUnread_returnsCount() {
        when(repo.countByUserIdAndRead("user-1", false)).thenReturn(3L);
        assertThat(service.countUnread("user-1")).isEqualTo(3L);
    }

    @Test
    void send_createsNotification() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Notification result = service.send("user-1", "ORDER_CONFIRMED", "Test mesaj");
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(result.isRead()).isFalse();
        verify(repo).save(any());
    }

    @Test
    void markRead_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Notification result = service.markRead(1L, "user-1");
        assertThat(result.isRead()).isTrue();
    }

    @Test
    void markRead_wrongUser_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> service.markRead(1L, "other-user"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void markRead_notFound_throws() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(99L, "user-1"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void markAllRead_marksAll() {
        Notification n2 = new Notification();
        n2.setUserId("user-1"); n2.setRead(false);
        when(repo.findByUserIdAndRead("user-1", false)).thenReturn(List.of(sample, n2));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.markAllRead("user-1");
        verify(repo, times(2)).save(any());
    }
}

