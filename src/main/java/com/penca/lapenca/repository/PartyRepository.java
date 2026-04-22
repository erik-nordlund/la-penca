package com.penca.lapenca.repository;

import com.penca.lapenca.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyRepository extends JpaRepository<Party, Long> {
    Optional<Party> findByCode(String code);
}