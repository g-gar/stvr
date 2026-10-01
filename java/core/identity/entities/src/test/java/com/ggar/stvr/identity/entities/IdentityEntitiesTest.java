package com.ggar.stvr.identity.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityEntitiesTest {

    @Test
    @DisplayName("UserId: should validate and provide conversion utilities")
    void shouldHandleUserId() {
        UUID uuid = UUID.randomUUID();
        UserId id = UserId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id.asString()).isEqualTo(uuid.toString());
        assertThat(UserId.fromString(uuid.toString())).isEqualTo(id);
        assertThat(UserId.random()).isNotNull();

        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Username: should validate length and alphanumeric characters")
    void shouldValidateUsername() {
        Username valid = Username.of("streamer123");
        assertThat(valid.value()).isEqualTo("streamer123");
        assertThat(valid.asString()).isEqualTo("streamer123");

        // Exactly 3 chars
        assertThat(Username.of("abc").value()).isEqualTo("abc");

        // Exactly 30 chars
        String thirtyChars = "a".repeat(30);
        assertThat(Username.of(thirtyChars).value()).isEqualTo(thirtyChars);

        // Null
        assertThatThrownBy(() -> Username.of(null))
                .isInstanceOf(NullPointerException.class);

        // Too short (< 3)
        assertThatThrownBy(() -> Username.of("ab"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 3 and 30 characters");

        // Too long (> 30)
        assertThatThrownBy(() -> Username.of("a".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 3 and 30 characters");

        // Non-alphanumeric characters
        assertThatThrownBy(() -> Username.of("user@name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alphanumeric");

        assertThatThrownBy(() -> Username.of("user name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alphanumeric");

        assertThatThrownBy(() -> Username.of("user!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alphanumeric");
    }

    @Test
    @DisplayName("User: should create user domain model with default creation timestamp")
    void shouldCreateUser() {
        UserId id = UserId.random();
        Username username = Username.of("adminUser");
        String hash = "$2a$12$dummyHashValue";
        Role role = Role.ADMIN;

        User user = new User(id, username, hash, role);

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.passwordHash()).isEqualTo(hash);
        assertThat(user.role()).isEqualTo(Role.ADMIN);
        assertThat(user.createdAt()).isNotNull();
    }
}
