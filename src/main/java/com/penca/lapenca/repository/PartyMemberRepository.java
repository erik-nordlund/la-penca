package com.penca.lapenca.repository;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    Optional<PartyMember> findFirstByUser(AppUser user);
}