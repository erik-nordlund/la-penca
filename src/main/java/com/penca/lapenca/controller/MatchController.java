package com.penca.lapenca.controller;

import com.penca.lapenca.entity.Match;
import com.penca.lapenca.repository.MatchRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MatchController {

    private final MatchRepository matchRepository;

    public MatchController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @GetMapping("/matches")
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }
}