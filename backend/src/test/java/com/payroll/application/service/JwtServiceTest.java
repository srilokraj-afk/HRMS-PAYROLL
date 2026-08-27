package com.payroll.application.service;

import com.payroll.application.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "test-secret-for-jwt-must-be-at-least-32-bytes-long";

    @Test
    void generatesAndValidatesToken() {
        JwtService jwtService = new JwtService(SECRET, 60);
        UserDetails user = User.withUsername("admin").password("x").roles("ADMIN").build();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void rejectsTokenForDifferentUser() {
        JwtService jwtService = new JwtService(SECRET, 60);
        UserDetails admin = User.withUsername("admin").password("x").roles("ADMIN").build();
        UserDetails user = User.withUsername("user").password("x").roles("USER").build();

        String token = jwtService.generateToken(admin);

        assertFalse(jwtService.isTokenValid(token, user));
    }
}
