package com.penca.lapenca.repository;

import com.penca.lapenca.entity.ActualQualifiedTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActualQualifiedTeamRepository extends JpaRepository<ActualQualifiedTeam, Long> {

    List<ActualQualifiedTeam> findByStage(String stage);
}