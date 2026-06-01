package com.example.userservice.repository;

import com.example.userservice.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
class UserProfileRepositoryTest {

    @Autowired UserProfileRepository repository;

    @Test
    void saveAndFindByUserId() {
        UserProfile p = new UserProfile();
        p.setUserId("u-001");
        p.setUsername("testuser");
        p.setEmail("test@test.com");
        repository.save(p);

        Optional<UserProfile> found = repository.findByUserId("u-001");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void existsByUserId_true() {
        UserProfile p = new UserProfile();
        p.setUserId("u-002");
        p.setUsername("u2");
        repository.save(p);
        assertThat(repository.existsByUserId("u-002")).isTrue();
    }

    @Test
    void existsByUserId_false() {
        assertThat(repository.existsByUserId("nonexistent")).isFalse();
    }

    @Test
    void findByUserId_notFound_returnsEmpty() {
        assertThat(repository.findByUserId("ghost")).isEmpty();
    }
}

