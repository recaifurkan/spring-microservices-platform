package com.example.authserver.controller;

import com.example.authserver.dto.UserRequest;
import com.example.authserver.dto.UserResponse;
import com.example.authserver.model.AppUser;
import com.example.authserver.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User management API — no restart is required; changes take effect immediately.
 *
 * Security: requires ROLE_ADMIN (via HTTP Basic or session cookie).
 *   curl example:
 *     curl -u admin:admin123 http://localhost:9000/admin/users
 *
 * Grant examples: SCOPE_read, SCOPE_write, ROLE_ADMIN, ROLE_MODERATOR
 */
@RestController
@RequestMapping("/admin/users")
public class UserManagementController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /admin/users — list all users
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    public List<UserResponse> listUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /admin/users/{username} — fetch a single user
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/{username}")
    public UserResponse getUser(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı: " + username));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /admin/users — create a new user
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping
    @Transactional
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Kullanıcı zaten mevcut: " + req.username());
        }

        AppUser user = new AppUser();
        user.setUsername(req.username());
        user.setPassword(encodePassword(req.password()));
        user.setEmail(req.email());
        user.setFullName(req.fullName());
        user.setEnabled(req.enabled() != null ? req.enabled() : Boolean.TRUE);
        user.setGrants(req.grants() != null ? req.grants() : defaultGrants());

        AppUser saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /admin/users/{username} — update a user (including password)
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/{username}")
    @Transactional
    public UserResponse updateUser(@PathVariable String username,
                                   @RequestBody UserRequest req) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı: " + username));

        if (req.password() != null && !req.password().isBlank()) {
            user.setPassword(encodePassword(req.password()));
        }
        if (req.email()    != null) user.setEmail(req.email());
        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.grants()   != null) user.setGrants(req.grants());
        user.setEnabled(req.enabled() != null ? req.enabled() : user.isEnabled());

        return UserResponse.from(userRepository.save(user));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /admin/users/{username}/grants — update grants only
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{username}/grants")
    @Transactional
    public UserResponse updateGrants(@PathVariable String username,
                                     @RequestBody Set<String> grants) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı: " + username));
        user.setGrants(grants);
        return UserResponse.from(userRepository.save(user));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /admin/users/{username}/enabled — enable or disable the user
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/{username}/enabled")
    @Transactional
    public UserResponse setEnabled(@PathVariable String username,
                                   @RequestParam boolean value) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı: " + username));
        user.setEnabled(value);
        return UserResponse.from(userRepository.save(user));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /admin/users/{username} — delete the user
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{username}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        if (!userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Kullanıcı bulunamadı: " + username);
        }
        userRepository.deleteByUsername(username);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Encodes the password with BCrypt. The {noop} prefix is also accepted. */
    private String encodePassword(String raw) {
        if (raw == null || raw.isBlank()) return "{noop}changeme";
        if (raw.startsWith("{")) return raw;   // already encoded ({noop}xxx, {bcrypt}xxx)
        return passwordEncoder.encode(raw);    // BCrypt
    }

    /** Default grants for new users */
    private Set<String> defaultGrants() {
        Set<String> grants = new HashSet<>();
        grants.add("SCOPE_read");
        return grants;
    }
}

