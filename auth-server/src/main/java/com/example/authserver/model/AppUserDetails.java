package com.example.authserver.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetails adapter.
 * Carries the DB-backed AppUser together with extra fields (email, fullName, id)
 * that the token customizer can access.
 */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final String email;
    private final String fullName;
    private final Set<GrantedAuthority> authorities;

    public AppUserDetails(AppUser user) {
        this.id          = user.getId();
        this.username    = user.getUsername();
        this.password    = user.getPassword();
        this.enabled     = user.isEnabled();
        this.email       = user.getEmail();
        this.fullName    = user.getFullName();
        this.authorities = user.getGrants().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    // ── Extra getters (used by the token customizer) ────────────────────────
    public Long getId()       { return id; }
    public String getEmail()    { return email; }
    public String getFullName() { return fullName; }

    // ── UserDetails ──────────────────────────────────────────────────────────
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()   { return password; }
    @Override public String getUsername()   { return username; }
    @Override public boolean isEnabled()    { return enabled; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}

