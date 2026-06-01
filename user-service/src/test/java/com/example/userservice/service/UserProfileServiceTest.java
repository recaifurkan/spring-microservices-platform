package com.example.userservice.service;

import com.example.userservice.model.UserProfile;
import com.example.userservice.repository.UserProfileRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock UserProfileRepository repository;
    @InjectMocks UserProfileService service;

    private UserProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = new UserProfile();
        sampleProfile.setUserId("user-123");
        sampleProfile.setUsername("testuser");
        sampleProfile.setEmail("test@example.com");
    }

    @Test
    void findAll_returnsAllProfiles() {
        when(repository.findAll()).thenReturn(List.of(sampleProfile));
        List<UserProfile> result = service.findAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user-123");
    }

    @Test
    void findByUserId_existingUser_returnsProfile() {
        when(repository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
        UserProfile result = service.findByUserId("user-123");
        assertThat(result.getUserId()).isEqualTo("user-123");
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void findByUserId_notFound_throwsException() {
        when(repository.findByUserId("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByUserId("unknown"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createOrUpdate_newUser_createsProfile() {
        when(repository.findByUserId("new-user")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.createOrUpdate(
            "new-user", "newuser", "new@example.com", "New User", "Bio", null);

        assertThat(result.getUserId()).isEqualTo("new-user");
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(repository).save(any());
    }

    @Test
    void createOrUpdate_existingUser_updatesProfile() {
        when(repository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.createOrUpdate(
            "user-123", null, null, "Updated Name", "Updated bio", null);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        verify(repository).save(any());
    }

    @Test
    void deleteByUserId_existingUser_deletes() {
        when(repository.findByUserId("user-123")).thenReturn(Optional.of(sampleProfile));
        service.deleteByUserId("user-123");
        verify(repository).delete(sampleProfile);
    }

    @Test
    void deleteByUserId_notFound_throwsException() {
        when(repository.findByUserId("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteByUserId("ghost"))
            .isInstanceOf(ResponseStatusException.class);
    }
}

