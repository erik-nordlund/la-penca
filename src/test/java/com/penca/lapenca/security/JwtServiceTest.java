package com.penca.lapenca.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

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