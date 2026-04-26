package com.penca.lapenca.repository;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.KnockoutPrediction;
import com.penca.lapenca.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnockoutPredictionRepository extends JpaRepository<KnockoutPrediction, Long> {

    Optional<KnockoutPrediction> findByUserAndPartyAndRoundNameAndMatchNumber(
            AppUser user,
            Party party,
            String roundName,
            int matchNumber
    );

    List<KnockoutPrediction> findByUserAndPartyAndRoundNameOrderByMatchNumberAsc(
            AppUser user,
            Party party,
            String roundName
    );
}