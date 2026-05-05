package com.penca.lapenca.repository;

import com.penca.lapenca.entity.ActualKnockoutResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActualKnockoutResultRepository extends JpaRepository<ActualKnockoutResult, Long> {

    List<ActualKnockoutResult> findByRoundNameOrderByMatchNumberAsc(String roundName);

    Optional<ActualKnockoutResult> findByRoundNameAndMatchNumber(String roundName, int matchNumber);

    void deleteByRoundName(String roundName);

    void deleteByRoundNameAndMatchNumber(String roundName, int matchNumber);
}