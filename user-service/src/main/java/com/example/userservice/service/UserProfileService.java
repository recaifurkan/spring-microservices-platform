package com.example.userservice.service;

import com.example.userservice.model.UserProfile;
import com.example.userservice.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserProfile> findAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public UserProfile findByUserId(String userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User profile not found: " + userId));
    }

    public UserProfile createOrUpdate(String userId, String username, String email,
                                      String fullName, String bio, String avatarUrl) {
        UserProfile profile = repository.findByUserId(userId)
                .orElse(new UserProfile());

        profile.setUserId(userId);
        if (username  != null) profile.setUsername(username);
        if (email     != null) profile.setEmail(email);
        if (fullName  != null) profile.setFullName(fullName);
        if (bio       != null) profile.setBio(bio);
        if (avatarUrl != null) profile.setAvatarUrl(avatarUrl);
        profile.setUpdatedAt(LocalDateTime.now());

        if (profile.getId() == null) profile.setCreatedAt(LocalDateTime.now());

        return repository.save(profile);
    }

    public void deleteByUserId(String userId) {
        UserProfile profile = findByUserId(userId);
        repository.delete(profile);
    }
}

