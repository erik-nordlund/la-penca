package com.penca.lapenca.service;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.PartyMember;
import com.penca.lapenca.repository.AppUserRepository;
import com.penca.lapenca.repository.PartyMemberRepository;
import com.penca.lapenca.repository.PartyRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PartyMemberService {

    private final PartyMemberRepository partyMemberRepository;
    private final PartyRepository partyRepository;
    private final AppUserRepository appUserRepository;

    public PartyMemberService(PartyMemberRepository partyMemberRepository,
                              PartyRepository partyRepository,
                              AppUserRepository appUserRepository) {
        this.partyMemberRepository = partyMemberRepository;
        this.partyRepository = partyRepository;
        this.appUserRepository = appUserRepository;
    }

    public PartyMember joinParty(String username, String partyCode) {
        username = username.trim();
        partyCode = partyCode.trim().toUpperCase();

        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }

        if (partyCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party code cannot be empty");
        }

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found"));

        if (partyMemberRepository.existsByUserAndParty(user, party)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already joined this party");
        }

        PartyMember partyMember = PartyMember.builder()
                .user(user)
                .party(party)
                .build();

        return partyMemberRepository.save(partyMember);
    }

    @Transactional
    public void leaveParty(String username, String partyCode) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found"));

        if (!partyMemberRepository.existsByUserAndParty(user, party)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not in this party");
        }

        partyMemberRepository.deleteByUserAndParty(user, party);
    }
}