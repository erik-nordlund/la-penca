package com.penca.lapenca.controller;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.repository.AppUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppUserController {

    private final AppUserRepository appUserRepository;

    public AppUserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/user/create")
    public AppUser createUser(@RequestParam String username) {
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