/*
package com.penca.lapenca.service;

import com.penca.lapenca.dto.ScoreBreakdownDto;
import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredictionScoreBreakdownTest {

    private PredictionRepository predictionRepository;
    private AppUserRepository appUserRepository;
    private PartyRepository partyRepository;
    private KnockoutPredictionRepository knockoutPredictionRepository;
    private ActualQualifiedTeamRepository actualQualifiedTeamRepository;

    private PredictionService predictionService;

    private AppUser user;
    private Party party;

    private Team argentina;
    private Team france;
    private Team brazil;
    private Team germany;

    @BeforeEach
    void setUp() {
        predictionRepository = mock(PredictionRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        partyRepository = mock(PartyRepository.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        knockoutPredictionRepository = mock(KnockoutPredictionRepository.class);
        actualQualifiedTeamRepository = mock(ActualQualifiedTeamRepository.class);

        predictionService = new PredictionService(
                predictionRepository,
                appUserRepository,
                partyRepository,
                matchRepository,
                mock(QualifiedThirdPlaceSelectionRepository.class),
                mock(TeamRepository.class),
                knockoutPredictionRepository,
                actualQualifiedTeamRepository,
                mock(PartyMemberRepository.class),
                mock(ActualKnockoutResultRepository.class),
                mock(GroupTieBreakRankingRepository.class)
        );

        user = AppUser.builder().id(1L).username("erik").build();
        party = Party.builder().id(1L).code("ABC123").build();

        argentina = Team.builder().id(1L).name("Argentina").groupName("A").build();
        france = Team.builder().id(2L).name("France").groupName("A").build();
        brazil = Team.builder().id(3L).name("Brazil").groupName("B").build();
        germany = Team.builder().id(4L).name("Germany").groupName("B").build();

        when(appUserRepository.findByUsername("erik")).thenReturn(Optional.of(user));
        when(partyRepository.findByCode("ABC123")).thenReturn(Optional.of(party));

        when(actualQualifiedTeamRepository.findByStage("ROUND_OF_32")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("ROUND_OF_16")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("QUARTER_FINAL")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("SEMI_FINAL")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("FINAL")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("CHAMPION")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("RUNNER_UP")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("THIRD_PLACE")).thenReturn(List.of());
        when(actualQualifiedTeamRepository.findByStage("FOURTH_PLACE")).thenReturn(List.of());

        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "ROUND_OF_32")).thenReturn(List.of());
        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "ROUND_OF_16")).thenReturn(List.of());
        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "QUARTER_FINAL")).thenReturn(List.of());
        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "SEMI_FINAL")).thenReturn(List.of());
        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "FINAL")).thenReturn(List.of());
        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "THIRD_PLACE")).thenReturn(List.of());
    }

    @Test
    void calculateUserScoreBreakdown_awardsGroupPointsForCorrectOutcomesOnly() {
        Match correctMatch = Match.builder()
                .stage("GROUP")
                .homeTeam(argentina)
                .awayTeam(france)
                .homeScore(2)
                .awayScore(1)
                .played(true)
                .build();

        Match wrongMatch = Match.builder()
                .stage("GROUP")
                .homeTeam(brazil)
                .awayTeam(germany)
                .homeScore(0)
                .awayScore(1)
                .played(true)
                .build();

        Match unplayedMatch = Match.builder()
                .stage("GROUP")
                .homeTeam(argentina)
                .awayTeam(brazil)
                .played(false)
                .build();

        Prediction correctPrediction = Prediction.builder()
                .user(user)
                .party(party)
                .match(correctMatch)
                .predictedOutcome(MatchOutcome.HOME_WIN)
                .build();

        Prediction wrongPrediction = Prediction.builder()
                .user(user)
                .party(party)
                .match(wrongMatch)
                .predictedOutcome(MatchOutcome.HOME_WIN)
                .build();

        Prediction unplayedPrediction = Prediction.builder()
                .user(user)
                .party(party)
                .match(unplayedMatch)
                .predictedOutcome(MatchOutcome.DRAW)
                .build();

        when(predictionRepository.findByUserAndParty(user, party))
                .thenReturn(List.of(correctPrediction, wrongPrediction, unplayedPrediction));

        ScoreBreakdownDto result = predictionService.calculateUserScoreBreakdown("erik", "ABC123");

        assertThat(result.getGroupPoints()).isEqualTo(1);
        assertThat(result.getTotalPoints()).isEqualTo(1);
    }

    @Test
    void calculateUserScoreBreakdown_awardsKnockoutStagePoints() {
        when(predictionRepository.findByUserAndParty(user, party)).thenReturn(List.of());

        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "ROUND_OF_32"))
                .thenReturn(List.of(knockoutPrediction("ROUND_OF_32", argentina)));

        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "ROUND_OF_16"))
                .thenReturn(List.of(knockoutPrediction("ROUND_OF_16", france)));

        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "QUARTER_FINAL"))
                .thenReturn(List.of(knockoutPrediction("QUARTER_FINAL", brazil)));

        when(actualQualifiedTeamRepository.findByStage("ROUND_OF_16"))
                .thenReturn(List.of(actualTeam(argentina, "ROUND_OF_16")));

        when(actualQualifiedTeamRepository.findByStage("QUARTER_FINAL"))
                .thenReturn(List.of(actualTeam(france, "QUARTER_FINAL")));

        when(actualQualifiedTeamRepository.findByStage("SEMI_FINAL"))
                .thenReturn(List.of(actualTeam(brazil, "SEMI_FINAL")));

        ScoreBreakdownDto result = predictionService.calculateUserScoreBreakdown("erik", "ABC123");

        assertThat(result.getRoundOf16Points()).isEqualTo(2);
        assertThat(result.getQuarterFinalPoints()).isEqualTo(3);
        assertThat(result.getSemiFinalPoints()).isEqualTo(4);
        assertThat(result.getTotalPoints()).isEqualTo(9);
    }

    @Test
    void calculateUserScoreBreakdown_awardsChampionPoints() {
        when(predictionRepository.findByUserAndParty(user, party)).thenReturn(List.of());

        when(knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "FINAL"))
                .thenReturn(List.of(knockoutPrediction("FINAL", argentina)));

        when(actualQualifiedTeamRepository.findByStage("CHAMPION"))
                .thenReturn(List.of(actualTeam(argentina, "CHAMPION")));

        ScoreBreakdownDto result = predictionService.calculateUserScoreBreakdown("erik", "ABC123");

        assertThat(result.getFinalPoints()).isEqualTo(6);
        assertThat(result.getTotalPoints()).isEqualTo(6);
    }

    private KnockoutPrediction knockoutPrediction(String roundName, Team winner) {
        return KnockoutPrediction.builder()
                .user(user)
                .party(party)
                .roundName(roundName)
                .predictedWinner(winner)
                .build();
    }

    private ActualQualifiedTeam actualTeam(Team team, String stage) {
        return ActualQualifiedTeam.builder()
                .team(team)
                .stage(stage)
                .build();
    }
}*/