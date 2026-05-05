package com.penca.lapenca.repository;

import com.penca.lapenca.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStage(String stage);
    List<Match> findByStageAndHomeTeam_GroupName(String stage, String groupName);
    List<Match> findByStageAndHomeTeam_GroupNameOrderByMatchDateAsc(String stage, String group);
}