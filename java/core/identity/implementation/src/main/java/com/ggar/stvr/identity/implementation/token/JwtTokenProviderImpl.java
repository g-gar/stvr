package com.ggar.stvr.identity.implementation.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggar.stvr.identity.api.RegisterUserCommandHandler.UserDto;
import com.ggar.stvr.identity.api.exception.InvalidTokenException;
import com.ggar.stvr.identity.api.token.AuthToken;
import com.ggar.stvr.identity.api.token.TokenProvider;
import com.ggar.stvr.identity.persistence.TokenRevocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard HMAC-SHA256 JWT TokenProvider implementation.
 * Includes jti (JWT ID), subject, username, role, and expiration claims,
 * along with token revocation and validation support.
 */
@Component
public class JwtTokenProviderImpl implements TokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();

    private static final String DEFAULT_SECRET = "stvr-super-secret-jwt-signing-key-for-development-and-testing-only-256bit!";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final byte[] secretKeyBytes;
    private final Duration tokenTtl;
    private final ObjectMapper objectMapper;
    private final TokenRevocationRepository revocationRepository;
    private final Set<String> localRevocationCache = ConcurrentHashMap.newKeySet();

    public JwtTokenProviderImpl() {
        this(DEFAULT_SECRET, DEFAULT_TTL, new ObjectMapper(), null);
    }

    @Autowired
    public JwtTokenProviderImpl(TokenRevocationRepository revocationRepository) {
        this(DEFAULT_SECRET, DEFAULT_TTL, new ObjectMapper(), revocationRepository);
    }

    public JwtTokenProviderImpl(String secretKey, Duration tokenTtl) {
        this(secretKey, tokenTtl, new ObjectMapper(), null);
    }

    public JwtTokenProviderImpl(String secretKey, Duration tokenTtl, TokenRevocationRepository revocationRepository) {
        this(secretKey, tokenTtl, new ObjectMapper(), revocationRepository);
    }

    public JwtTokenProviderImpl(
            String secretKey,
            Duration tokenTtl,
            ObjectMapper objectMapper,
            TokenRevocationRepository revocationRepository
    ) {
        Objects.requireNonNull(secretKey, "secretKey cannot be null");
        this.secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.tokenTtl = Objects.requireNonNull(tokenTtl, "tokenTtl cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.revocationRepository = revocationRepository;
    }

    @Override
    public AuthToken issueToken(UserDto user) {
        Objects.requireNonNull(user, "User cannot be null");

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtl);

        try {
            // 1. Header
            String headerJson = objectMapper.writeValueAsString(Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            ));
            String encodedHeader = B64_ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));

            // 2. Payload with claims (including jti for revocation/logout tracking)
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                    "sub", user.id().asString(),
                    "username", user.username().asString(),
                    "role", user.role().name(),
                    "jti", UUID.randomUUID().toString(),
                    "iat", issuedAt.getEpochSecond(),
                    "exp", expiresAt.getEpochSecond()
            ));
            String encodedPayload = B64_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // 3. Signature
            String signatureContent = encodedHeader + "." + encodedPayload;
            String encodedSignature = sign(signatureContent);

            String tokenValue = signatureContent + "." + encodedSignature;
            return new AuthToken(tokenValue, "Bearer", issuedAt, expiresAt);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue JWT token", e);
        }
    }

    @Override
    public boolean validateToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return false;
        }

        String[] parts = tokenValue.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            // 1. Verify HMAC signature in constant time
            String signatureContent = parts[0] + "." + parts[1];
            String expectedSignature = sign(signatureContent);

            byte[] expectedSigBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] actualSigBytes = parts[2].getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expectedSigBytes, actualSigBytes)) {
                return false;
            }

            // 2. Parse payload claims
            byte[] payloadBytes = B64_DECODER.decode(parts[1]);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);

            // 3. Verify expiration
            if (!payloadNode.has("exp")) {
                return false;
            }
            long expEpoch = payloadNode.get("exp").asLong();
            long currentEpoch = Instant.now().getEpochSecond();
            if (expEpoch < currentEpoch) {
                return false;
            }

            // 4. Verify revocation status via jti
            if (payloadNode.has("jti")) {
                String jti = payloadNode.get("jti").asText();
                if (localRevocationCache.contains(jti)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Mono<Void> revokeToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            return Mono.error(new InvalidTokenException(tokenValue, "Token cannot be null or blank"));
        }

        String[] parts = tokenValue.split("\\.");
        if (parts.length != 3) {
            return Mono.error(new InvalidTokenException(tokenValue, "Malformed token structure"));
        }

        try {
            // Verify signature before attempting revocation
            String signatureContent = parts[0] + "." + parts[1];
            String expectedSignature = sign(signatureContent);

            byte[] expectedSigBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] actualSigBytes = parts[2].getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expectedSigBytes, actualSigBytes)) {
                return Mono.error(new InvalidTokenException(tokenValue, "Invalid token signature"));
            }

            byte[] payloadBytes = B64_DECODER.decode(parts[1]);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);

            if (!payloadNode.has("jti")) {
                return Mono.error(new InvalidTokenException(tokenValue, "Token missing required jti claim"));
            }

            String jti = payloadNode.get("jti").asText();
            long expEpoch = payloadNode.has("exp") ? payloadNode.get("exp").asLong() : Instant.now().plus(tokenTtl).getEpochSecond();
            Instant expiresAt = Instant.ofEpochSecond(expEpoch);

            // Record in L1 memory cache
            localRevocationCache.add(jti);

            // Record in L2 persistent repository if available
            if (revocationRepository != null) {
                return revocationRepository.revoke(jti, expiresAt);
            }

            return Mono.empty();
        } catch (InvalidTokenException e) {
            return Mono.error(e);
        } catch (Exception e) {
            return Mono.error(new InvalidTokenException(tokenValue, e));
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return B64_ENCODER.encodeToString(rawHmac);
    }
}
