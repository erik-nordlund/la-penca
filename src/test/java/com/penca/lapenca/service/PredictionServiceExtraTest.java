/*
package com.penca.lapenca.service;

import com.penca.lapenca.dto.GroupTableRow;
import com.penca.lapenca.dto.ThirdPlaceSelectionDto;
import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PredictionServiceExtraTest {

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

    private AppUser user;
    private AppUser otherUser;
    private Party party;

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

        user = AppUser.builder().id(1L).username("erik").build();
        otherUser = AppUser.builder().id(2L).username("alex").build();
        party = Party.builder().id(1L).code("ABC123").predictionDeadline(LocalDateTime.now().plusDays(1)).build();

        when(appUserRepository.findByUsername("erik")).thenReturn(Optional.of(user));
        when(appUserRepository.findByUsername("alex")).thenReturn(Optional.of(otherUser));
        when(partyRepository.findByCode("ABC123")).thenReturn(Optional.of(party));
    }

    @Test
    void saveGroupTieBreakRanking_savesFourTeamsAndClearsBracket() {
        Team a = team("Argentina", "A");
        Team b = team("Brazil", "A");
        Team c = team("Chile", "A");
        Team d = team("Denmark", "A");

        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(a));
        when(teamRepository.findByName("Brazil")).thenReturn(Optional.of(b));
        when(teamRepository.findByName("Chile")).thenReturn(Optional.of(c));
        when(teamRepository.findByName("Denmark")).thenReturn(Optional.of(d));

        when(groupTieBreakRankingRepository.save(any(GroupTieBreakRanking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<GroupTieBreakRanking> saved = predictionService.saveGroupTieBreakRanking(
                "erik",
                "ABC123",
                "A",
                List.of("Argentina", "Brazil", "Chile", "Denmark")
        );

        assertThat(saved).hasSize(4);
        assertThat(saved.get(0).getPositionIndex()).isEqualTo(1);
        assertThat(saved.get(0).getTeam().getName()).isEqualTo("Argentina");
        assertThat(saved.get(3).getPositionIndex()).isEqualTo(4);
        assertThat(saved.get(3).getTeam().getName()).isEqualTo("Denmark");

        verify(groupTieBreakRankingRepository).deleteByUserAndPartyAndGroupName(user, party, "A");
        verify(qualifiedThirdPlaceSelectionRepository).deleteByUserAndParty(user, party);
        verify(knockoutPredictionRepository).deleteByUserAndParty(user, party);
    }

    @Test
    void saveGroupTieBreakRanking_throwsWhenNotExactlyFourTeams() {
        assertThatThrownBy(() -> predictionService.saveGroupTieBreakRanking(
                "erik",
                "ABC123",
                "A",
                List.of("Argentina", "Brazil")
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You must rank exactly 4 teams");

        verify(groupTieBreakRankingRepository, never()).save(any());
    }

    @Test
    void calculateGroupTable_usesSavedTieBreakRankingWhenTeamsHaveSamePoints() {
        Team argentina = team("Argentina", "A");
        Team brazil = team("Brazil", "A");

        Match match = Match.builder()
                .id(1L)
                .stage("GROUP")
                .homeTeam(argentina)
                .awayTeam(brazil)
                .build();

        Prediction prediction = Prediction.builder()
                .user(user)
                .party(party)
                .match(match)
                .predictedOutcome(MatchOutcome.DRAW)
                .build();

        GroupTieBreakRanking brazilFirst = GroupTieBreakRanking.builder()
                .user(user)
                .party(party)
                .groupName("A")
                .team(brazil)
                .positionIndex(1)
                .build();

        GroupTieBreakRanking argentinaSecond = GroupTieBreakRanking.builder()
                .user(user)
                .party(party)
                .groupName("A")
                .team(argentina)
                .positionIndex(2)
                .build();

        when(predictionRepository.findByUserAndParty(user, party)).thenReturn(List.of(prediction));
        when(groupTieBreakRankingRepository.findByUserAndPartyAndGroupNameOrderByPositionIndexAsc(user, party, "A"))
                .thenReturn(List.of(brazilFirst, argentinaSecond));

        List<GroupTableRow> table = predictionService.calculateGroupTable("erik", "ABC123", "A");

        assertThat(table).hasSize(2);
        assertThat(table.get(0).getTeamName()).isEqualTo("Brazil");
        assertThat(table.get(1).getTeamName()).isEqualTo("Argentina");
    }

    @Test
    void saveKnockoutPrediction_whenRoundOf16WinnerChanges_clearsAffectedFutureRounds() {
        Team france = team("France", "D");

        when(teamRepository.findByName("France")).thenReturn(Optional.of(france));
        when(knockoutPredictionRepository.findByUserAndPartyAndRoundNameAndMatchNumber(user, party, "ROUND_OF_16", 3))
                .thenReturn(Optional.empty());

        when(knockoutPredictionRepository.save(any(KnockoutPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        predictionService.saveKnockoutPrediction(
                "erik",
                "ABC123",
                "ROUND_OF_16",
                3,
                "Winner R32-5 vs Winner R32-6",
                "France"
        );

        verify(knockoutPredictionRepository).deleteByUserAndPartyAndRoundNameAndMatchNumber(user, party, "QUARTER_FINAL", 2);
        verify(knockoutPredictionRepository).deleteByUserAndPartyAndRoundNameAndMatchNumber(user, party, "SEMI_FINAL", 1);
        verify(knockoutPredictionRepository).deleteByUserAndPartyAndRoundNameAndMatchNumber(user, party, "FINAL", 1);
        verify(knockoutPredictionRepository).deleteByUserAndPartyAndRoundNameAndMatchNumber(user, party, "THIRD_PLACE", 1);
    }

    @Test
    void validateCanViewPredictions_beforeDeadline_blocksViewingOtherUsers() {
        party.setPredictionDeadline(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> predictionService.validateCanViewPredictions("erik", "alex", "ABC123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You can only view other users' predictions after the party deadline");
    }

    @Test
    void validateCanViewPredictions_afterDeadline_allowsViewingOtherUsers() {
        party.setPredictionDeadline(LocalDateTime.now().minusDays(1));

        assertThatCode(() -> predictionService.validateCanViewPredictions("erik", "alex", "ABC123"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCanViewPredictions_sameUserIsAlwaysAllowedBeforeDeadline() {
        party.setPredictionDeadline(LocalDateTime.now().plusDays(1));

        assertThatCode(() -> predictionService.validateCanViewPredictions("erik", "erik", "ABC123"))
                .doesNotThrowAnyException();
    }

    @Test
    void getThirdPlaceSelection_whenThereIsCutoffTie_returnsAutomaticAndTiedTeams() {
        PredictionService spyService = spy(predictionService);

        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
        int[] thirdPlacePoints = {9, 8, 7, 6, 5, 4, 3, 3, 3, 2, 1, 0};

        for (int i = 0; i < groups.size(); i++) {
            String group = groups.get(i);
            int points = thirdPlacePoints[i];

            doReturn(List.of(
                    row(group + "1", 10),
                    row(group + "2", 9),
                    row(group + "3", points)
            )).when(spyService).calculateGroupTable("erik", "ABC123", group);
        }

        ThirdPlaceSelectionDto result = spyService.getThirdPlaceSelection("erik", "ABC123");

        assertThat(result.getQualifiedAutomatically()).hasSize(6);
        assertThat(result.getTiedTeams()).hasSize(3);
        assertThat(result.getRemainingSlots()).isEqualTo(2);
    }

    @Test
    void saveQualifiedThirdPlaceTeams_throwsWhenNotExactlyEightTeams() {
        assertThatThrownBy(() -> predictionService.saveQualifiedThirdPlaceTeams(
                "erik",
                "ABC123",
                List.of("Argentina", "Brazil")
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You must select exactly 8 third-place teams");

        verify(qualifiedThirdPlaceSelectionRepository, never()).save(any());
    }

    @Test
    void saveQualifiedThirdPlaceTeams_savesEightTeamsAndClearsKnockoutPredictions() {
        List<Team> teams = List.of(
                team("A3", "A"),
                team("B3", "B"),
                team("C3", "C"),
                team("D3", "D"),
                team("E3", "E"),
                team("F3", "F"),
                team("G3", "G"),
                team("H3", "H")
        );

        for (Team team : teams) {
            when(teamRepository.findByName(team.getName())).thenReturn(Optional.of(team));
        }

        when(qualifiedThirdPlaceSelectionRepository.save(any(QualifiedThirdPlaceSelection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> saved = predictionService.saveQualifiedThirdPlaceTeams(
                "erik",
                "ABC123",
                teams.stream().map(Team::getName).toList()
        );

        assertThat(saved).hasSize(8);
        verify(qualifiedThirdPlaceSelectionRepository).deleteByUserAndParty(user, party);
        verify(qualifiedThirdPlaceSelectionRepository, times(8)).save(any(QualifiedThirdPlaceSelection.class));
        verify(knockoutPredictionRepository).deleteByUserAndParty(user, party);
    }

    @Test
    void saveActualKnockoutResult_finalCreatesChampionAndRunnerUp() {
        Team argentina = team("Argentina", "A");
        Team france = team("France", "B");

        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(argentina));
        when(teamRepository.findByName("France")).thenReturn(Optional.of(france));

        AtomicReference<ActualKnockoutResult> savedResult = new AtomicReference<>();

        when(actualKnockoutResultRepository.findByRoundNameAndMatchNumber("FINAL", 1))
                .thenReturn(Optional.empty());

        when(actualKnockoutResultRepository.saveAndFlush(any(ActualKnockoutResult.class)))
                .thenAnswer(invocation -> {
                    ActualKnockoutResult result = invocation.getArgument(0);
                    savedResult.set(result);
                    return result;
                });

        when(actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc("FINAL"))
                .thenAnswer(invocation -> List.of(savedResult.get()));

        when(actualQualifiedTeamRepository.save(any(ActualQualifiedTeam.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        predictionService.saveActualKnockoutResult(
                "FINAL",
                1,
                "Winner SF-1 vs Winner SF-2",
                "Argentina",
                "France",
                2,
                1,
                null,
                null
        );

        verify(actualQualifiedTeamRepository, atLeastOnce()).deleteByStage("CHAMPION");
        verify(actualQualifiedTeamRepository, atLeastOnce()).deleteByStage("RUNNER_UP");

        verify(actualQualifiedTeamRepository).save(argThat(actual ->
                actual.getStage().equals("CHAMPION") &&
                        actual.getTeam().getName().equals("Argentina")
        ));

        verify(actualQualifiedTeamRepository).save(argThat(actual ->
                actual.getStage().equals("RUNNER_UP") &&
                        actual.getTeam().getName().equals("France")
        ));
    }

    @Test
    void saveActualKnockoutResult_thirdPlaceCreatesThirdAndFourthPlace() {
        Team croatia = team("Croatia", "C");
        Team morocco = team("Morocco", "D");

        when(teamRepository.findByName("Croatia")).thenReturn(Optional.of(croatia));
        when(teamRepository.findByName("Morocco")).thenReturn(Optional.of(morocco));

        AtomicReference<ActualKnockoutResult> savedResult = new AtomicReference<>();

        when(actualKnockoutResultRepository.findByRoundNameAndMatchNumber("THIRD_PLACE", 1))
                .thenReturn(Optional.empty());

        when(actualKnockoutResultRepository.saveAndFlush(any(ActualKnockoutResult.class)))
                .thenAnswer(invocation -> {
                    ActualKnockoutResult result = invocation.getArgument(0);
                    savedResult.set(result);
                    return result;
                });

        when(actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc("THIRD_PLACE"))
                .thenAnswer(invocation -> List.of(savedResult.get()));

        when(actualQualifiedTeamRepository.save(any(ActualQualifiedTeam.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        predictionService.saveActualKnockoutResult(
                "THIRD_PLACE",
                1,
                "Loser SF-1 vs Loser SF-2",
                "Croatia",
                "Morocco",
                2,
                1,
                null,
                null
        );

        verify(actualQualifiedTeamRepository, atLeastOnce()).deleteByStage("THIRD_PLACE");
        verify(actualQualifiedTeamRepository, atLeastOnce()).deleteByStage("FOURTH_PLACE");

        verify(actualQualifiedTeamRepository).save(argThat(actual ->
                actual.getStage().equals("THIRD_PLACE") &&
                        actual.getTeam().getName().equals("Croatia")
        ));

        verify(actualQualifiedTeamRepository).save(argThat(actual ->
                actual.getStage().equals("FOURTH_PLACE") &&
                        actual.getTeam().getName().equals("Morocco")
        ));
    }

    private Team team(String name, String groupName) {
        return Team.builder()
                .name(name)
                .groupName(groupName)
                .build();
    }

    private GroupTableRow row(String teamName, int points) {
        return new GroupTableRow(teamName, 0, 0, 0, 0, points);
    }
}*/