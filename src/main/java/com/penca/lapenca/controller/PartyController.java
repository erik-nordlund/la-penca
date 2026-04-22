package com.penca.lapenca.controller;

import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.PartyMember;
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

    public PartyController(PartyService partyService, PartyMemberService partyMemberService) {
        this.partyService = partyService;
        this.partyMemberService = partyMemberService;
    }

    @GetMapping("/party/create")
    public Party createParty() {
        return partyService.createParty();
    }

    @GetMapping("/party/join")
    public PartyMember joinParty(@RequestParam String username, @RequestParam String code) {
        return partyMemberService.joinParty(username, code);
    }
}