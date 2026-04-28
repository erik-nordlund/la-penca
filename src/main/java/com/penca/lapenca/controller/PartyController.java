package com.penca.lapenca.controller;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.PartyMember;
import com.penca.lapenca.repository.AppUserRepository;
import com.penca.lapenca.repository.PartyMemberRepository;
import com.penca.lapenca.service.PartyMemberService;
import com.penca.lapenca.service.PartyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PartyController {

    private final PartyService partyService;
    private final PartyMemberService partyMemberService;
    private final AppUserRepository appUserRepository;
    private final PartyMemberRepository partyMemberRepository;

    public PartyController(PartyService partyService,
                           PartyMemberService partyMemberService,
                           AppUserRepository appUserRepository,
                           PartyMemberRepository partyMemberRepository) {
        this.partyService = partyService;
        this.partyMemberService = partyMemberService;
        this.appUserRepository = appUserRepository;
        this.partyMemberRepository = partyMemberRepository;
    }

    @GetMapping("/party/create")
    public Party createParty() {
        return partyService.createParty();
    }

    @GetMapping("/party/join")
    public PartyMember joinParty(@RequestParam String username, @RequestParam String code) {
        return partyMemberService.joinParty(username, code);
    }

    @GetMapping("/user/party")
    public Party getUserParty(@RequestParam String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return partyMemberRepository.findFirstByUser(user)
                .map(PartyMember::getParty)
                .orElse(null);
    }
}