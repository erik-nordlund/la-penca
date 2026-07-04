/*
package com.penca.lapenca.service;

import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActualKnockoutResultTest {

    private TeamRepository teamRepository;
    private ActualKnockoutResultRepository actualKnockoutResultRepository;
    private ActualQualifiedTeamRepository actualQualifiedTeamRepository;
    private PredictionService predictionService;

    private Team argentina;
    private Team france;

    @BeforeEach
    void setUp() {
        PredictionRepository predictionRepository = mock(PredictionRepository.class);
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        PartyRepository partyRepository = mock(PartyRepository.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        QualifiedThirdPlaceSelectionRepository qualifiedThirdPlaceSelectionRepository = mock(QualifiedThirdPlaceSelectionRepository.class);
        teamRepository = mock(TeamRepository.class);
        KnockoutPredictionRepository knockoutPredictionRepository = mock(KnockoutPredictionRepository.class);
        actualQualifiedTeamRepository = mock(ActualQualifiedTeamRepository.class);
        PartyMemberRepository partyMemberRepository = mock(PartyMemberRepository.class);
        actualKnockoutResultRepository = mock(ActualKnockoutResultRepository.class);
        GroupTieBreakRankingRepository groupTieBreakRankingRepository = mock(GroupTieBreakRankingRepository.class);

        predictionService = new PredictionService(
                predictionRepository,
                appUserRepository,
                partyRepository,
                matchRepository,
                qualifiedThirdPlaceSelectionRepository,
                teamRepository,
                knockoutPredictionRepository,
                actualQualifiedTeamRepository,
                partyMemberRepository,
                actualKnockoutResultRepository,
                groupTieBreakRankingRepository
        );

        argentina = Team.builder().id(1L).name("Argentina").groupName("A").build();
        france = Team.builder().id(2L).name("France").groupName("B").build();

        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(argentina));
        when(teamRepository.findByName("France")).thenReturn(Optional.of(france));

        when(actualKnockoutResultRepository.findByRoundNameAndMatchNumber("FINAL", 1))
                .thenReturn(Optional.empty());

        when(actualKnockoutResultRepository.saveAndFlush(any(ActualKnockoutResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc("FINAL"))
                .thenAnswer(invocation -> List.of());
    }

    @Test
    void savesNormalKnockoutWinWithoutPenaltyScores() {
        ActualKnockoutResult result = predictionService.saveActualKnockoutResult(
                "FINAL",
                1,
                "Winner SF-1 vs Winner SF-2",
                "Argentina",
                "France",
                2,
                1,
                5,
                4
        );

        assertThat(result.getWinner().getName()).isEqualTo("Argentina");
        assertThat(result.getHomeScore()).isEqualTo(2);
        assertThat(result.getAwayScore()).isEqualTo(1);
        assertThat(result.getHomePenaltyScore()).isNull();
        assertThat(result.getAwayPenaltyScore()).isNull();
        assertThat(result.isPlayed()).isTrue();
    }

    @Test
    void savesPenaltyWinWhenMatchScoreIsTied() {
        ActualKnockoutResult result = predictionService.saveActualKnockoutResult(
                "FINAL",
                1,
                "Winner SF-1 vs Winner SF-2",
                "Argentina",
                "France",
                1,
                1,
                4,
                3
        );

        assertThat(result.getWinner().getName()).isEqualTo("Argentina");
        assertThat(result.getHomeScore()).isEqualTo(1);
        assertThat(result.getAwayScore()).isEqualTo(1);
        assertThat(result.getHomePenaltyScore()).isEqualTo(4);
        assertThat(result.getAwayPenaltyScore()).isEqualTo(3);
    }

    @Test
    void throwsWhenTiedMatchHasNoPenaltyScore() {
        assertThatThrownBy(() -> predictionService.saveActualKnockoutResult(
                "FINAL",
                1,
                "Winner SF-1 vs Winner SF-2",
                "Argentina",
                "France",
                1,
                1,
                null,
                null
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Penalty score is required");
    }

    @Test
    void throwsWhenPenaltyScoreIsAlsoTied() {
        assertThatThrownBy(() -> predictionService.saveActualKnockoutResult(
                "FINAL",
                1,
                "Winner SF-1 vs Winner SF-2",
                "Argentina",
                "France",
                1,
                1,
                4,
                4
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Penalty score cannot be tied");
    }
}*/