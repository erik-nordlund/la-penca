/*
package com.penca.lapenca.service;

import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredictionServiceTest {

    private PredictionRepository predictionRepository;
    private AppUserRepository appUserRepository;
    private PartyRepository partyRepository;
    private MatchRepository matchRepository;
    private QualifiedThirdPlaceSelectionRepository qualifiedThirdPlaceSelectionRepository;
    private TeamRepository teamRepository;
    private KnockoutPredictionRepository knockoutPredictionRepository;
    private ActualQualifiedTeamRepository actualQualifiedTeamRepository;
    private PartyMemberRepository partyMemberRepository;
    private ActualKnockoutResultRepository actualKnockoutResultRepository;
    private GroupTieBreakRankingRepository groupTieBreakRankingRepository;

    private PredictionService predictionService;

    private Team homeTeam;
    private Team awayTeam;

    @BeforeEach
    void setUp() {
        predictionRepository = mock(PredictionRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        partyRepository = mock(PartyRepository.class);
        matchRepository = mock(MatchRepository.class);
        qualifiedThirdPlaceSelectionRepository = mock(QualifiedThirdPlaceSelectionRepository.class);
        teamRepository = mock(TeamRepository.class);
        knockoutPredictionRepository = mock(KnockoutPredictionRepository.class);
        actualQualifiedTeamRepository = mock(ActualQualifiedTeamRepository.class);
        partyMemberRepository = mock(PartyMemberRepository.class);
        actualKnockoutResultRepository = mock(ActualKnockoutResultRepository.class);
        groupTieBreakRankingRepository = mock(GroupTieBreakRankingRepository.class);

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

        homeTeam = Team.builder().id(1L).name("Norway").groupName("A").build();
        awayTeam = Team.builder().id(2L).name("Sweden").groupName("B").build();

        when(teamRepository.findByName("Norway")).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findByName("Sweden")).thenReturn(Optional.of(awayTeam));

        AtomicReference<ActualKnockoutResult> savedResult = new AtomicReference<>();

        when(actualKnockoutResultRepository.saveAndFlush(any(ActualKnockoutResult.class)))
                .thenAnswer(invocation -> {
                    ActualKnockoutResult result = invocation.getArgument(0);
                    savedResult.set(result);
                    return result;
                });

        when(actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc(anyString()))
                .thenAnswer(invocation -> {
                    ActualKnockoutResult result = savedResult.get();
                    return result == null ? List.of() : List.of(result);
                });

        when(actualQualifiedTeamRepository.save(any(ActualQualifiedTeam.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveActualKnockoutResult_whenHomeWins_savesHomeAsWinnerAndClearsPenalties() {
        ActualKnockoutResult result = predictionService.saveActualKnockoutResult(
                "ROUND_OF_32",
                1,
                "1A vs 2B",
                "Norway",
                "Sweden",
                2,
                1,
                5,
                4
        );

        assertEquals(homeTeam, result.getWinner());
        assertEquals(2, result.getHomeScore());
        assertEquals(1, result.getAwayScore());
        assertNull(result.getHomePenaltyScore());
        assertNull(result.getAwayPenaltyScore());
        assertTrue(result.isPlayed());

        ArgumentCaptor<ActualQualifiedTeam> captor = ArgumentCaptor.forClass(ActualQualifiedTeam.class);
        verify(actualQualifiedTeamRepository).save(captor.capture());

        assertEquals("ROUND_OF_16", captor.getValue().getStage());
        assertEquals(homeTeam, captor.getValue().getTeam());
    }

    @Test
    void saveActualKnockoutResult_whenAwayWins_savesAwayAsWinner() {
        ActualKnockoutResult result = predictionService.saveActualKnockoutResult(
                "ROUND_OF_32",
                1,
                "1A vs 2B",
                "Norway",
                "Sweden",
                0,
                3,
                null,
                null
        );

        assertEquals(awayTeam, result.getWinner());
        assertEquals(0, result.getHomeScore());
        assertEquals(3, result.getAwayScore());
        assertTrue(result.isPlayed());
    }

    @Test
    void saveActualKnockoutResult_whenDrawWithoutPenalties_throwsError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                predictionService.saveActualKnockoutResult(
                        "ROUND_OF_32",
                        1,
                        "1A vs 2B",
                        "Norway",
                        "Sweden",
                        1,
                        1,
                        null,
                        null
                )
        );

        assertEquals("Penalty score is required when knockout match is tied", exception.getMessage());
        verify(actualKnockoutResultRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveActualKnockoutResult_whenPenaltyScoreIsTied_throwsError() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                predictionService.saveActualKnockoutResult(
                        "ROUND_OF_32",
                        1,
                        "1A vs 2B",
                        "Norway",
                        "Sweden",
                        1,
                        1,
                        4,
                        4
                )
        );

        assertEquals("Penalty score cannot be tied", exception.getMessage());
        verify(actualKnockoutResultRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveActualKnockoutResult_whenDrawAndAwayWinsPenalties_savesAwayAsWinner() {
        ActualKnockoutResult result = predictionService.saveActualKnockoutResult(
                "ROUND_OF_32",
                1,
                "1A vs 2B",
                "Norway",
                "Sweden",
                1,
                1,
                3,
                5
        );

        assertEquals(awayTeam, result.getWinner());
        assertEquals(1, result.getHomeScore());
        assertEquals(1, result.getAwayScore());
        assertEquals(3, result.getHomePenaltyScore());
        assertEquals(5, result.getAwayPenaltyScore());
        assertTrue(result.isPlayed());
    }

    @Test
    void resetActualKnockoutResult_clearsScoresPenaltiesWinnerAndPlayed() {
        ActualKnockoutResult existing = ActualKnockoutResult.builder()
                .roundName("ROUND_OF_32")
                .matchNumber(1)
                .slot("1A vs 2B")
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .homeScore(1)
                .awayScore(1)
                .homePenaltyScore(3)
                .awayPenaltyScore(5)
                .winner(awayTeam)
                .played(true)
                .build();

        when(actualKnockoutResultRepository.findByRoundNameAndMatchNumber("ROUND_OF_32", 1))
                .thenReturn(Optional.of(existing));

        ActualKnockoutResult result = predictionService.resetActualKnockoutResult("ROUND_OF_32", 1);

        assertNull(result.getHomeScore());
        assertNull(result.getAwayScore());
        assertNull(result.getHomePenaltyScore());
        assertNull(result.getAwayPenaltyScore());
        assertNull(result.getWinner());
        assertFalse(result.isPlayed());
    }

    @Test
    void saveActualKnockoutResult_whenRoundOf32Changes_clearsFutureRoundsAndStages() {
        predictionService.saveActualKnockoutResult(
                "ROUND_OF_32",
                1,
                "1A vs 2B",
                "Norway",
                "Sweden",
                2,
                1,
                null,
                null
        );

        verify(actualKnockoutResultRepository).deleteByRoundName("ROUND_OF_16");
        verify(actualKnockoutResultRepository).deleteByRoundName("QUARTER_FINAL");
        verify(actualKnockoutResultRepository).deleteByRoundName("SEMI_FINAL");
        verify(actualKnockoutResultRepository).deleteByRoundName("FINAL");
        verify(actualKnockoutResultRepository).deleteByRoundName("THIRD_PLACE");

        verify(actualQualifiedTeamRepository, atLeastOnce()).deleteByStage("ROUND_OF_16");
        verify(actualQualifiedTeamRepository).deleteByStage("QUARTER_FINAL");
        verify(actualQualifiedTeamRepository).deleteByStage("SEMI_FINAL");
        verify(actualQualifiedTeamRepository).deleteByStage("FINAL");
        verify(actualQualifiedTeamRepository).deleteByStage("CHAMPION");
        verify(actualQualifiedTeamRepository).deleteByStage("RUNNER_UP");
        verify(actualQualifiedTeamRepository).deleteByStage("THIRD_PLACE");
        verify(actualQualifiedTeamRepository).deleteByStage("FOURTH_PLACE");
    }
}*/