package com.penca.lapenca.repository;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Match;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    Optional<Prediction> findByUserAndPartyAndMatch(AppUser user, Party party, Match match);
    List<Prediction> findByUserAndParty(AppUser user, Party party);
}