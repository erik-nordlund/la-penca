package com.penca.lapenca.service;

import com.penca.lapenca.entity.Party;
import com.penca.lapenca.repository.PartyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PartyService {

    private final PartyRepository partyRepository;

    public PartyService(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public Party createParty(String name, LocalDateTime deadline) {
        Party party = Party.builder()
                .code(generateUniquePartyCode())
                .name(name)
                .createdAt(LocalDateTime.now())
                .predictionDeadline(deadline)
                .build();

        return partyRepository.save(party);
    }

    public Party setPredictionDeadline(String code, LocalDateTime deadline) {
        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        party.setPredictionDeadline(deadline);

        return partyRepository.save(party);
    }

    public Optional<Party> findByCode(String code) {
        return partyRepository.findByCode(code);
    }

    public boolean isPredictionLocked(String code) {
        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return party.getPredictionDeadline() != null
                && LocalDateTime.now().isAfter(party.getPredictionDeadline());
    }

    private String generateUniquePartyCode() {
        String code;

        do {
            code = generatePartyCode();
        } while (partyRepository.findByCode(code).isPresent());

        return code;
    }

    private String generatePartyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }
}