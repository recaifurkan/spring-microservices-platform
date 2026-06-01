package com.example.userservice.controller;

import com.example.userservice.dto.UpsertProfileRequest;
import com.example.userservice.model.UserProfile;
import com.example.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Service", description = "Kullanıcı profil yönetimi")
public class UserProfileController {

    private final UserProfileService service;
    public UserProfileController(UserProfileService service) { this.service = service; }

    @Operation(summary = "Tüm kullanıcıları listele (users:manage — ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_users:manage')")
    public List<UserProfile> listAll() { return service.findAll(); }

    @Operation(summary = "Kendi profilini getir", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/profile")
    public UserProfile getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return service.findByUserId(jwt.getSubject());
    }

    @Operation(summary = "Kullanıcı profilini getir (ID ile — ADMIN veya kendiniz)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_users:manage') or authentication.name == #userId")
    public UserProfile getProfile(@PathVariable String userId) {
        return service.findByUserId(userId);
    }

    @Operation(summary = "Profil oluştur/güncelle", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/profile")
    public ResponseEntity<UserProfile> upsertProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpsertProfileRequest req) {
        UserProfile saved = service.createOrUpdate(
                jwt.getSubject(),
                req.username() != null ? req.username() : jwt.getSubject(),
                req.email() != null ? req.email() : jwt.getClaimAsString("email"),
                req.fullName(),
                req.bio(),
                req.avatarUrl()
        );
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Profili sil (users:manage veya kendiniz)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_users:manage') or authentication.name == #userId")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        service.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
