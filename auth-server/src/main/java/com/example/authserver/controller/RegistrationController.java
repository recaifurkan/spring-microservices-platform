package com.example.authserver.controller;

import com.example.authserver.dto.RegisterRequest;
import com.example.authserver.dto.UserResponse;
import com.example.authserver.model.AppUser;
import com.example.authserver.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@RestController
public class RegistrationController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest req) {
        if (req.username() == null || req.username().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        if (req.password() == null || req.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long");

        if (userRepository.existsByUsername(req.username().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username '" + req.username() + "' is already taken");

        if (req.email() != null && !req.email().isBlank() && userRepository.existsByEmail(req.email().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email address is already registered");

        AppUser user = new AppUser();
        user.setUsername(req.username().trim());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setEmail(req.email() != null ? req.email().trim() : null);
        user.setFullName(req.fullName() != null ? req.fullName().trim() : null);
        user.setEnabled(true);
        user.setGrants(Set.of(
            "ROLE_USER",
            "SCOPE_catalog:read",
            "SCOPE_orders:read",    "SCOPE_orders:write",
            "SCOPE_cart:read",      "SCOPE_cart:write",
            "SCOPE_payments:read",  "SCOPE_payments:write",
            "SCOPE_users:read",     "SCOPE_users:write",
            "SCOPE_notifications:read", "SCOPE_notifications:write"
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userRepository.save(user)));
    }
}
