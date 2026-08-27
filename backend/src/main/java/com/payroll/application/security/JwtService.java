package com.payroll.application.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:60}")
            long expirationMinutes) {

        if (secret == null ||
                secret.getBytes(StandardCharsets.UTF_8).length < 32) {

            throw new IllegalArgumentException(
                    "app.jwt.secret must be at least 32 bytes long"
            );
        }

        this.signingKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );

        this.expiration =
                Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(UserDetails userDetails) {

        Instant now = Instant.now();

        String role = userDetails
                .getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority()
                                .replace("ROLE_", "")
                )
                .orElse("USER");

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plus(expiration)
                        )
                )
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        try {

            Claims claims = parseClaims(token);

            return claims.getSubject()
                    .equals(userDetails.getUsername())

                    && claims.getExpiration()
                    .after(new Date());

        } catch (RuntimeException exception) {

            return false;
        }
    }

    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}