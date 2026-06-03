package tramplin.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    /**
     * Небезопасный дефолт из application.yml — допустим только для локальной разработки.
     * На проде использование этого ключа означает, что токены подписываются публично
     * известным секретом из репозитория, поэтому старт под prod-профилем запрещён.
     */
    static final String INSECURE_DEFAULT_SECRET = "my-super-secret-key-for-jwt-256-bits-long!!";

    /** HS256 требует ключ не короче 256 бит = 32 байт. */
    private static final int MIN_SECRET_LENGTH_BYTES = 32;

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
            Environment environment
    ) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod) {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "JWT_SECRET не задан. На проде секрет обязателен — задайте переменную окружения JWT_SECRET (>= "
                                + MIN_SECRET_LENGTH_BYTES + " символов).");
            }
            if (INSECURE_DEFAULT_SECRET.equals(secret)) {
                throw new IllegalStateException(
                        "JWT_SECRET равен дефолтному ключу из application.yml. Этот ключ публично известен — "
                                + "на проде задайте собственный JWT_SECRET.");
            }
            if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "JWT_SECRET слишком короткий для HS256: нужно минимум "
                                + MIN_SECRET_LENGTH_BYTES + " байт.");
            }
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, accessExpirationMs, "access");
    }

    public String generateRefreshToken(UUID userId, String email, String role) {
        return buildToken(userId, email, role, refreshExpirationMs, "refresh");
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT истёк: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Невалидный JWT: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Неверная подпись JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Пустой JWT: {}", e.getMessage());
        }
        return false;
    }

    private String buildToken(UUID userId, String email, String role,
                              long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
