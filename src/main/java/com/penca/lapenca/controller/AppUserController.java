package com.penca.lapenca.controller;

import com.penca.lapenca.dto.AuthRequestDto;
import com.penca.lapenca.dto.AuthResponseDto;
import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.repository.AppUserRepository;
import com.penca.lapenca.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AppUserController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AppUserController(AppUserRepository appUserRepository,
                             PasswordEncoder passwordEncoder,
                             JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponseDto register(@RequestBody AuthRequestDto request) {
        String username = normalize(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }

        if (password == null || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        if (appUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (!email.isBlank() && appUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        AppUser user = AppUser.builder()
                .username(username)
                .email(email.isBlank() ? null : email)
                .passwordHash(passwordEncoder.encode(password))
                .role("USER")
                .totalPoints(0)
                .build();

        AppUser saved = appUserRepository.save(user);

        return toResponse(saved);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody AuthRequestDto request) {
        String username = normalize(request.getUsername());
        String password = request.getPassword();

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return toResponse(user);
    }

    private AuthResponseDto toResponse(AppUser user) {

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole()
        );

        return new AuthResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}