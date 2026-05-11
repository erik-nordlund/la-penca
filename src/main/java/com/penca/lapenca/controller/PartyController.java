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

    @GetMapping("/party/create")
    public Party createParty(@RequestParam String username,
                             @RequestParam String name) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyService.createParty(name.trim());

        PartyMember partyMember = PartyMember.builder()
                .user(user)
                .party(party)
                .build();

        partyMemberRepository.save(partyMember);

        return party;
    }

    @GetMapping("/party/join")
    public PartyMember joinParty(@RequestParam String username, @RequestParam String code) {
        return partyMemberService.joinParty(username, code);
    }

    @GetMapping("/user/party")
    public ResponseEntity<Party> getUserParty(@RequestParam String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return partyMemberRepository.findFirstByUser(user)
                .map(member -> ResponseEntity.ok(member.getParty()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/party/my-parties")
    public List<PartyDto> getMyParties(@RequestParam String username) {
        AppUser user = appUserRepository.findByUsername(username)
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
    public String leaveParty(@RequestParam String username,
                             @RequestParam String code) {
        partyMemberService.leaveParty(username, code);
        return "Left party";
    }

    @PostMapping("/party/deadline/set")
    public Party setPredictionDeadline(@RequestParam String adminUsername,
                                       @RequestParam String code,
                                       @RequestParam String deadline) {
        requireAdmin(adminUsername);
        return partyService.setPredictionDeadline(
                code,
                LocalDateTime.parse(deadline)
        );
    }

    @GetMapping("/party/lock-status")
    public boolean getLockStatus(@RequestParam String code) {
        return partyService.isPredictionLocked(code);
    }
    private void requireAdmin(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}