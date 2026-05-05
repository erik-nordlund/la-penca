package com.penca.lapenca.repository;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    Optional<PartyMember> findFirstByUser(AppUser user);

    List<PartyMember> findByParty(Party party);

    List<PartyMember> findByUser(AppUser user);

    boolean existsByUserAndParty(AppUser user, Party party);

    void deleteByUserAndParty(AppUser user, Party party);
}