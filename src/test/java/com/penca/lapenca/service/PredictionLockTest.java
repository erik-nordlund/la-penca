/*package com.penca.lapenca.service;

import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredictionLockTest {

    private PredictionRepository predictionRepository;
    private AppUserRepository appUserRepository;
    private PartyRepository partyRepository;
    private MatchRepository matchRepository;
    private PredictionService predictionService;

    @BeforeEach
    void setUp() {
        predictionRepository = mock(PredictionRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        partyRepository = mock(PartyRepository.class);
        matchRepository = mock(MatchRepository.class);

        predictionService = new PredictionService(
                predictionRepository,
                appUserRepository,
                partyRepository,
                matchRepository,
                mock(QualifiedThirdPlaceSelectionRepository.class),
                mock(TeamRepository.class),
                mock(KnockoutPredictionRepository.class),
                mock(ActualQualifiedTeamRepository.class),
                mock(PartyMemberRepository.class),
                mock(ActualKnockoutResultRepository.class),
                mock(GroupTieBreakRankingRepository.class)
        );
    }

    @Test
    void savePredictionShouldFailWhenPartyDeadlineHasPassed() {
        AppUser user = AppUser.builder().id(1L).username("erik").build();

        Party party = Party.builder()
                .id(1L)
                .code("ABC123")
                .predictionDeadline(LocalDateTime.now().minusDays(1))
                .build();

        Team home = Team.builder().name("Argentina").groupName("A").build();
        Team away = Team.builder().name("France").groupName("A").build();

        Match match = Match.builder()
                .id(1L)
                .homeTeam(home)
                .awayTeam(away)
                .stage("GROUP")
                .build();

        when(appUserRepository.findByUsername("erik")).thenReturn(Optional.of(user));
        when(partyRepository.findByCode("ABC123")).thenReturn(Optional.of(party));
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> predictionService.savePrediction(
                "erik",
                "ABC123",
                1L,
                MatchOutcome.HOME_WIN
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Predictions are locked");
    }
}*/