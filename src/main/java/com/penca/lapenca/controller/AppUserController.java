package com.penca.lapenca.controller;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AppUserController {

    private final AppUserRepository appUserRepository;

    public AppUserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/user/create")
    public AppUser createUser(@RequestParam String username) {
        username = username.trim();

        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }

        if (appUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        AppUser user = AppUser.builder()
                .username(username)
                .email(username + "@test.com")
                .password("test123")
                .role("USER")
                .totalPoints(0)
                .build();

        return appUserRepository.save(user);
    }
}