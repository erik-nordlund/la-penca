package com.penca.lapenca.repository;

import com.penca.lapenca.entity.AppUser;
import com.penca.lapenca.entity.Party;
import com.penca.lapenca.entity.QualifiedThirdPlaceSelection;
import com.penca.lapenca.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualifiedThirdPlaceSelectionRepository extends JpaRepository<QualifiedThirdPlaceSelection, Long> {
    List<QualifiedThirdPlaceSelection> findByUserAndParty(
            AppUser user,
            Party party
    );
    Optional<QualifiedThirdPlaceSelection> findByUserAndPartyAndTeam(
            AppUser user,
            Party party,
            Team team
    );
    void deleteByUserAndParty(
            AppUser user,
            Party party
    );
}