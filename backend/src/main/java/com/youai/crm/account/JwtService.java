package com.youai.crm.account;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-hours:2}") long accessTtlHours,
            @Value("${security.jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofHours(accessTtlHours);
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    /** Issues the short-lived bearer token used for API requests. */
    public String issue(CrmUser user) {
        return issueAccess(user);
    }

    public String issueAccess(CrmUser user) {
        return issue(user, ACCESS_TOKEN_TYPE, accessTtl);
    }

    public String issueRefresh(CrmUser user) {
        return issue(user, REFRESH_TOKEN_TYPE, refreshTtl);
    }

    private String issue(CrmUser user, String tokenType, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .claim("displayName", user.getDisplayName())
                .claim("roles", user.getRoles().stream().map(Role::getCode).toList())
                .claim("credentialVersion", user.getCredentialVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Instant expiresAt(String token) {
        return parse(token).getExpiration().toInstant();
    }

    public String tokenType(Claims claims) {
        Object value = claims.get(TOKEN_TYPE_CLAIM);
        return value == null ? null : value.toString();
    }
}

