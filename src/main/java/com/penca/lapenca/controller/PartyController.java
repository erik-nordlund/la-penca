package com.penca.lapenca.controller;

import com.penca.lapenca.dto.PartyDto;
import com.penca.lapenca.dto.PartyMemberDto;
import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.PartyMember;
import com.penca.lapenca.repository.AppUserRepository;
import com.penca.lapenca.repository.PartyMemberRepository;
import com.penca.lapenca.service.PartyMemberService;
import com.penca.lapenca.service.PartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

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

    @PostMapping("/party/create")
    public Party createParty(Authentication authentication,
                             @RequestParam String name) {

        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyService.createParty(name.trim());

        PartyMember partyMember = PartyMember.builder()
                .user(user)
                .party(party)
                .build();

        partyMemberRepository.save(partyMember);

        return party;
    }

    @PostMapping("/party/join")
    public PartyMember joinParty(Authentication authentication,
                                 @RequestParam String code) {
        return partyMemberService.joinParty(authentication.getName(), code);
    }

    @GetMapping("/user/party")
    public ResponseEntity<Party> getUserParty(Authentication authentication) {
        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return partyMemberRepository.findFirstByUser(user)
                .map(member -> ResponseEntity.ok(member.getParty()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/party/my-parties")
    public List<PartyDto> getMyParties(Authentication authentication) {
        AppUser user = appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return partyMemberRepository.findByUser(user)
                .stream()
                .map(member -> {
                    Party party = member.getParty();
                    int memberCount = partyMemberRepository.findByParty(party).size();

                    return new PartyDto(
                            party.getCode(),
                            party.getName(),
                            party.getPredictionDeadline(),
                            memberCount
                    );
                })
                .toList();
    }

    @GetMapping("/party/members")
    public List<PartyMemberDto> getPartyMembers(@RequestParam String code) {
        Party party = partyService.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return partyMemberRepository.findByParty(party)
                .stream()
                .map(member -> new PartyMemberDto(member.getUser().getUsername()))
                .toList();
    }

    @PostMapping("/party/leave")
    public String leaveParty(Authentication authentication,
                             @RequestParam String code) {
        partyMemberService.leaveParty(authentication.getName(), code);
        return "Left party";
    }

    @PostMapping("/party/deadline/set")
    public Party setPredictionDeadline(@RequestParam String code,
                                       @RequestParam String deadline) {
        return partyService.setPredictionDeadline(
                code,
                LocalDateTime.parse(deadline)
        );
    }

    @GetMapping("/party/lock-status")
    public boolean getLockStatus(@RequestParam String code)
    {
        return partyService.isPredictionLocked(code);
    }
}