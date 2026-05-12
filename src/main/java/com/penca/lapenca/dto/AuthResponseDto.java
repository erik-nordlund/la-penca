package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponseDto {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String token;
    private String refreshToken;
}