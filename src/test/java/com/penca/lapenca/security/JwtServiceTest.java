package com.penca.lapenca.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                "this-is-a-test-secret-key-that-is-long-enough-for-jwt-signing"
        );
    }

    @Test
    void generatedTokenShouldBeValidForSameUsername() {
        String token = jwtService.generateToken("erik", "USER");

        assertThat(jwtService.extractUsername(token)).isEqualTo("erik");
        assertThat(jwtService.isValid(token, "erik")).isTrue();
    }

    @Test
    void generatedTokenShouldNotBeValidForDifferentUsername() {
        String token = jwtService.generateToken("erik", "USER");

        assertThat(jwtService.isValid(token, "otherUser")).isFalse();
    }
}