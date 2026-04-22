package com.penca.lapenca.repository;

import com.penca.lapenca.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}