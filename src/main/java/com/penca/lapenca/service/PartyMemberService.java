package com.penca.lapenca.service;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.PartyMember;
import com.penca.lapenca.repository.AppUserRepository;
import com.penca.lapenca.repository.PartyMemberRepository;
import com.penca.lapenca.repository.PartyRepository;
import org.springframework.stereotype.Service;

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
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        PartyMember partyMember = PartyMember.builder()
                .user(user)
                .party(party)
                .build();

        return partyMemberRepository.save(partyMember);
    }
}