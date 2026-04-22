package com.penca.lapenca.repository;

import com.penca.lapenca.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}