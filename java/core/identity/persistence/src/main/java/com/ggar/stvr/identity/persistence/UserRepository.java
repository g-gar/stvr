package com.ggar.stvr.identity.persistence;

import com.ggar.stvr.identity.entities.User;
import com.ggar.stvr.identity.entities.UserId;
import com.ggar.stvr.identity.entities.Username;
import reactor.core.publisher.Mono;

/**
 * Reactive persistence repository port for User identity storage and queries.
 */
public interface UserRepository {

    /**
     * Checks if a user already exists with the given username.
     *
     * @param username The username to check.
     * @return Mono emitting true if exists, false otherwise.
     */
    Mono<Boolean> existsByUsername(Username username);

    /**
     * Persists a new or updated user domain entity.
     *
     * @param user The user domain entity to save.
     * @return Mono emitting the persisted User entity.
     */
    Mono<User> save(User user);

    /**
     * Finds a user domain entity by its username.
     *
     * @param username The username to look up.
     * @return Mono emitting the found User, or Mono.empty() if not found.
     */
    Mono<User> findByUsername(Username username);

    /**
     * Finds a user domain entity by its unique ID.
     *
     * @param id The user identifier.
     * @return Mono emitting the found User, or Mono.empty() if not found.
     */
    Mono<User> findById(UserId id);
}
