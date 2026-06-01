package com.example.authserver.controller;

import com.example.authserver.dto.UserRequest;
import com.example.authserver.model.AppUser;
import com.example.authserver.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserManagementController controller;

    @Test
    void givenUsersWhenListingThenReturnsMappedResponses() {
        AppUser user = new AppUser();
        user.setUsername("alice");
        user.setPassword("hash");
        user.setGrants(Set.of("ROLE_USER"));
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(controller.listUsers()).hasSize(1);
        assertThat(controller.listUsers().get(0).username()).isEqualTo("alice");
    }

    @Test
    void givenExistingUserWhenFetchingThenReturnsResponse() {
        AppUser user = new AppUser();
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(controller.getUser("alice").username()).isEqualTo("alice");
    }

    @Test
    void givenMissingUserWhenFetchingThenThrowsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> controller.getUser("ghost"));
        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenNewUserRequestWhenCreatingThenUsesEncodedPasswordAndDefaultGrants() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.createUser(new UserRequest("bob", "secret123", "bob@example.com", "Bob", null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(captor.getValue().getGrants()).containsExactly("SCOPE_read");
    }

    @Test
    void givenExistingUserWhenCreatingThenThrowsConflict() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);
        Throwable thrown = catchThrowable(() -> controller.createUser(new UserRequest("bob", "secret123", null, null, null, null)));
        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void givenUpdateRequestWhenUpdatingThenChangesOnlyProvidedFields() {
        AppUser user = new AppUser();
        user.setUsername("bob");
        user.setPassword("old");
        user.setEmail("old@example.com");
        user.setEnabled(true);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.updateUser("bob", new UserRequest("bob", "newpass", "new@example.com", null, false, Set.of("ROLE_ADMIN")));

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.enabled()).isFalse();
        assertThat(response.grants()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void givenGrantUpdateWhenUpdatingThenPersistsNewGrants() {
        AppUser user = new AppUser();
        user.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.updateGrants("bob", Set.of("SCOPE_a", "SCOPE_b"));

        assertThat(response.grants()).containsExactlyInAnyOrder("SCOPE_a", "SCOPE_b");
    }

    @Test
    void givenEnableRequestWhenUpdatingThenPersistsEnabledFlag() {
        AppUser user = new AppUser();
        user.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.setEnabled("bob", false);

        assertThat(response.enabled()).isFalse();
    }

    @Test
    void givenExistingUserWhenDeletingThenReturnsNoContent() {
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        ResponseEntity<Void> response = controller.deleteUser("bob");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userRepository).deleteByUsername("bob");
    }
}

