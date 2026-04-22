package com.penca.lapenca.service;

import com.penca.lapenca.entity.Party;
import com.penca.lapenca.repository.PartyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PartyService{

    private final PartyRepository partyRepository;

    public PartyService(PartyRepository partyRepository){
        this.partyRepository = partyRepository;
    }

    public Party createParty(){
        Party party = Party.builder()
                .code(generatePartyCode())
                .createdAt(LocalDateTime.now())
                .build();

        return partyRepository.save(party);
    }

    public Optional<Party> findByCode(String code) {
        return partyRepository.findByCode(code);
    }

    private String generatePartyCode(){
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++){
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }
}