package com.penca.lapenca.repository;

import com.penca.lapenca.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupTieBreakRankingRepository extends JpaRepository<GroupTieBreakRanking, Long> {

    List<GroupTieBreakRanking> findByUserAndPartyAndGroupNameOrderByPositionIndexAsc(
            AppUser user,
            Party party,
            String groupName
    );

    void deleteByUserAndPartyAndGroupName(
            AppUser user,
            Party party,
            String groupName
    );

    void deleteByUserAndParty(
            AppUser user,
            Party party
    );
}