package com.securebank.securebank.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 32-byte key (256 bits) required for HMAC-SHA256
        String secret = Base64.getEncoder()
                .encodeToString("my-32-byte-secret-key-for-tests!".getBytes());
        ReflectionTestUtils.setField(jwtUtil, "secretKey", secret);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
    }

    private UserDetails user(String email) {
        return new User(email, "password", Collections.emptyList());
    }

    @Test
    void generateToken_extractUsername_roundTrip() {
        UserDetails ud = user("test@example.com");
        String token = jwtUtil.generateToken(ud);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_withMatchingUser_returnsTrue() {
        UserDetails ud = user("test@example.com");
        String token = jwtUtil.generateToken(ud);
        assertThat(jwtUtil.isTokenValid(token, ud)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUser_returnsFalse() {
        UserDetails ud = user("test@example.com");
        UserDetails other = user("other@example.com");
        String token = jwtUtil.generateToken(ud);
        assertThat(jwtUtil.isTokenValid(token, other)).isFalse();
    }

    @Test
    void generateToken_producesNonEmptyString() {
        String token = jwtUtil.generateToken(user("abc@example.com"));
        assertThat(token).isNotBlank();
        // JWT has exactly 3 dot-separated parts
        assertThat(token.split("\\.")).hasSize(3);
    }
}
