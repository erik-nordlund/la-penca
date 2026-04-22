package com.penca.lapenca.repository;

import com.penca.lapenca.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {
}