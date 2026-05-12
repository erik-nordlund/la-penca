package com.penca.lapenca.controller;

import com.penca.lapenca.dto.*;
import com.penca.lapenca.entity.*;
import com.penca.lapenca.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/prediction/save")
    public Prediction savePrediction(Authentication authentication,
                                     @RequestParam String code,
                                     @RequestParam Long matchId,
                                     @RequestParam MatchOutcome outcome) {
        return predictionService.savePrediction(authentication.getName(), code, matchId, outcome);
    }
    @GetMapping("/group-table")
    public List<GroupTableRow> getGroupTable(@RequestParam String viewerUsername,
                                             @RequestParam String username,
                                             @RequestParam String code,
                                             @RequestParam String group) {
        predictionService.validateCanViewPredictions(viewerUsername, username, code);
        return predictionService.calculateGroupTable(username, code, group);
    }
    @GetMapping("/qualified-teams")
    public QualifiedTeamsDto getQualifiedTeams(@RequestParam String username,
                                               @RequestParam String code,
                                               @RequestParam String group) {
        return predictionService.getQualifiedTeams(username, code, group);
    }
    @GetMapping("/group-result")
    public GroupResultDto getGroupResult(@RequestParam String username,
                                         @RequestParam String code,
                                         @RequestParam String group) {
        return predictionService.getGroupResult(username, code, group);
    }

    @GetMapping("/group-results")
    public List<GroupResultDto> getAllGroupResults(@RequestParam String username,
                                                   @RequestParam String code) {
        return predictionService.getAllGroupResults(username, code);
    }
    @GetMapping("/third-place-teams")
    public List<ThirdPlaceTeamDto> getAllThirdPlaceTeams(@RequestParam String username,
                                                         @RequestParam String code) {
        return predictionService.getAllThirdPlaceTeams(username, code);
    }
    @PostMapping("/prediction/auto-group")
    public List<Prediction> autoPredictGroup(Authentication authentication,
                                             @RequestParam String code,
                                             @RequestParam String group) {
        return predictionService.autoPredictGroup(authentication.getName(), code, group);
    }
    @GetMapping("/third-place-selection")
    public ThirdPlaceSelectionDto getThirdPlaceSelection(@RequestParam String viewerUsername,
                                                         @RequestParam String username,
                                                         @RequestParam String code) {
        predictionService.validateCanViewPredictions(viewerUsername, username, code);
        return predictionService.getThirdPlaceSelection(username, code);
    }
    @PostMapping("/third-place-selection/save")
    public List<String> saveQualifiedThirdPlaceTeams(Authentication authentication,
                                                     @RequestParam String code,
                                                     @RequestParam List<String> teams) {
        return predictionService.saveQualifiedThirdPlaceTeams(authentication.getName(), code, teams);
    }

    @GetMapping("/qualified-teams-overview")
    public QualifiedTeamsOverviewDto getQualifiedTeamsOverview(@RequestParam String username,
                                                               @RequestParam String code) {
        return predictionService.getQualifiedTeamsOverview(username, code);
    }
    @GetMapping("/test-rules")
    public List<ThirdPlaceRule> testRules() {
        return predictionService.loadRules();
    }
    @GetMapping("/third-place-rule")
    public ThirdPlaceRule getThirdPlaceRule(@RequestParam String username,
                                            @RequestParam String code) {
        return predictionService.findMatchingThirdPlaceRule(username, code);
    }
    @GetMapping("/round-of-32")
    public List<KnockoutMatchDto> getRoundOf32(@RequestParam String viewerUsername,
                                               @RequestParam String username,
                                               @RequestParam String code) {
        predictionService.validateCanViewPredictions(viewerUsername, username, code);
        return predictionService.buildRoundOf32(username, code);
    }
    @PostMapping("/knockout/save")
    public KnockoutPrediction saveKnockoutPrediction(Authentication authentication,
                                                     @RequestParam String code,
                                                     @RequestParam String round,
                                                     @RequestParam int matchNumber,
                                                     @RequestParam String slot,
                                                     @RequestParam String winner) {
        return predictionService.saveKnockoutPrediction(
                authentication.getName(),
                code,
                round,
                matchNumber,
                slot,
                winner
        );
    }

    @PostMapping("/knockout/auto-round-of-32")
    public List<KnockoutPrediction> autoPickRoundOf32HomeTeams(Authentication authentication,
                                                               @RequestParam String code) {
        return predictionService.autoPickRoundOf32HomeTeams(authentication.getName(), code);
    }

    @GetMapping("/round-of-16")
    public List<KnockoutMatchDto> getRoundOf16(@RequestParam String username,
                                               @RequestParam String code) {
        return predictionService.buildRoundOf16(username, code);
    }
    @PostMapping("/knockout/auto-round")
    public List<KnockoutPrediction> autoPickRound(Authentication authentication,
                                                  @RequestParam String code,
                                                  @RequestParam String round) {
        return predictionService.autoPickRound(authentication.getName(), code, round);
    }
    @GetMapping("/quarter-finals")
    public List<KnockoutMatchDto> getQuarterFinals(@RequestParam String username,
                                                   @RequestParam String code) {
        return predictionService.buildQuarterFinals(username, code);
    }

    @GetMapping("/semi-finals")
    public List<KnockoutMatchDto> getSemiFinals(@RequestParam String username,
                                                @RequestParam String code) {
        return predictionService.buildSemiFinals(username, code);
    }

    @GetMapping("/final")
    public List<KnockoutMatchDto> getFinal(@RequestParam String username,
                                           @RequestParam String code) {
        return predictionService.buildFinal(username, code);
    }
    @GetMapping("/knockout/bracket")
    public KnockoutBracketDto getBracket(@RequestParam String username,
                                         @RequestParam String code) {
        return predictionService.getFullBracket(username, code);
    }
    @GetMapping("/predictions")
    public List<Prediction> getPredictions(@RequestParam String viewerUsername,
                                           @RequestParam String username,
                                           @RequestParam String code) {
        return predictionService.getPredictions(viewerUsername, username, code);
    }
    @GetMapping("/knockout/predictions")
    public List<KnockoutPrediction> getKnockoutPredictions(@RequestParam String viewerUsername,
                                                           @RequestParam String username,
                                                           @RequestParam String code) {
        return predictionService.getKnockoutPredictions(viewerUsername, username, code);
    }
    @GetMapping("/third-place-teams/saved")
    public List<ThirdPlaceTeamDto> getSavedThirdPlaceTeams(@RequestParam String viewerUsername,
                                                           @RequestParam String username,
                                                           @RequestParam String code) {
        predictionService.validateCanViewPredictions(viewerUsername, username, code);
        return predictionService.getSavedThirdPlaceTeams(username, code);
    }
    @GetMapping("/score")
    public int getScore(@RequestParam String username,
                        @RequestParam String code) {
        return predictionService.calculateUserScore(username, code);
    }
    @PostMapping("/actual/qualified/add")
    public ActualQualifiedTeam addActualQualifiedTeam(@RequestParam String team,
                                                      @RequestParam String stage) {
        return predictionService.addActualQualifiedTeam(team, stage);
    }
    @PostMapping("/actual/match-result")
    public Match setActualMatchResult(@RequestParam Long matchId,
                                      @RequestParam int homeScore,
                                      @RequestParam int awayScore) {
        return predictionService.setActualMatchResult(matchId, homeScore, awayScore);
    }
    @GetMapping("/leaderboard")
    public List<LeaderboardRowDto> getLeaderboard(@RequestParam String code) {
        return predictionService.getLeaderboard(code);
    }
    @GetMapping("/third-place-match")
    public List<KnockoutMatchDto> getThirdPlaceMatch(@RequestParam String username,
                                                     @RequestParam String code) {
        return predictionService.buildThirdPlaceMatch(username, code);
    }
    @PostMapping("/actual/reset")
    public String resetActualData() {
        return predictionService.resetActualData();
    }
    @GetMapping("/score-breakdown")
    public ScoreBreakdownDto getScoreBreakdown(@RequestParam String username,
                                               @RequestParam String code) {
        return predictionService.calculateUserScoreBreakdown(username, code);
    }
    @PostMapping("/actual/match-result/reset")
    public Match resetActualMatchResult(@RequestParam Long matchId) {
        return predictionService.resetActualMatchResult(matchId);
    }

    @GetMapping("/matches/group")
    public List<Match> getGroupMatches(@RequestParam String group) {

        return predictionService.getGroupMatches(group);
    }

    @PostMapping("/actual/group/reset")
    public String resetActualGroupResults(@RequestParam String group) {
        return predictionService.resetActualGroupResults(group);
    }
    @GetMapping("/actual/group-table")
    public List<GroupTableRow> getActualGroupTable(@RequestParam String group) {
        return predictionService.calculateActualGroupTable(group);
    }

    @GetMapping("/actual/third-place-teams")
    public List<ThirdPlaceTeamDto> getActualThirdPlaceTeams() {
        return predictionService.getActualThirdPlaceTeams();
    }

    @PostMapping("/actual/round-of-32/save")
    public List<ActualQualifiedTeam> saveActualRoundOf32Teams(@RequestParam List<String> teams) {
        return predictionService.saveActualRoundOf32Teams(teams);
    }

    @GetMapping("/actual/qualified")
    public List<ActualQualifiedTeam> getActualQualifiedTeams(@RequestParam String stage) {
        return predictionService.getActualQualifiedTeamsByStage(stage);
    }

    @PostMapping("/actual/qualified/reset-stage")
    public String resetActualQualifiedTeamsByStage(@RequestParam String stage) {
        return predictionService.resetActualQualifiedTeamsByStage(stage);
    }

    @PostMapping("/actual/qualified/delete")
    public String deleteActualQualifiedTeam(@RequestParam Long id) {
        return predictionService.deleteActualQualifiedTeam(id);
    }
    @GetMapping("/actual/knockout/round")
    public List<KnockoutMatchDto> getActualKnockoutRound(@RequestParam String round) {
        return predictionService.buildActualKnockoutRound(round);
    }

    @GetMapping("/actual/knockout/results")
    public List<ActualKnockoutResult> getActualKnockoutResults(@RequestParam String round) {
        return predictionService.getActualKnockoutResults(round);
    }

    @PostMapping("/actual/knockout/save")
    public ActualKnockoutResult saveActualKnockoutResult(@RequestParam String round,
                                                         @RequestParam int matchNumber,
                                                         @RequestParam String slot,
                                                         @RequestParam String homeTeam,
                                                         @RequestParam String awayTeam,
                                                         @RequestParam int homeScore,
                                                         @RequestParam int awayScore,
                                                         @RequestParam(required = false) Integer homePenaltyScore,
                                                         @RequestParam(required = false) Integer awayPenaltyScore) {
        return predictionService.saveActualKnockoutResult(
                round,
                matchNumber,
                slot,
                homeTeam,
                awayTeam,
                homeScore,
                awayScore,
                homePenaltyScore,
                awayPenaltyScore
        );
    }

    @PostMapping("/actual/knockout/reset")
    public ActualKnockoutResult resetActualKnockoutResult(@RequestParam String round,
                                                          @RequestParam int matchNumber) {
        return predictionService.resetActualKnockoutResult(round, matchNumber);
    }
    @PostMapping("/group-tiebreak/save")
    public List<GroupTieBreakRanking> saveGroupTieBreakRanking(Authentication authentication,
                                                               @RequestParam String code,
                                                               @RequestParam String group,
                                                               @RequestParam List<String> teams) {
        return predictionService.saveGroupTieBreakRanking(
                authentication.getName(),
                code,
                group,
                teams
        );
    }

    @GetMapping("/group-tiebreak")
    public List<String> getGroupTieBreakRanking(@RequestParam String username,
                                                @RequestParam String code,
                                                @RequestParam String group) {
        return predictionService.getGroupTieBreakRanking(username, code, group);
    }
}