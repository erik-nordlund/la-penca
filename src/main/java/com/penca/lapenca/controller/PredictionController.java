package com.penca.lapenca.controller;

import com.penca.lapenca.dto.*;
import com.penca.lapenca.entity.KnockoutPrediction;
import com.penca.lapenca.entity.MatchOutcome;
import com.penca.lapenca.entity.Prediction;
import com.penca.lapenca.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/prediction/save")
    public Prediction savePrediction(@RequestParam String username,
                                     @RequestParam String code,
                                     @RequestParam Long matchId,
                                     @RequestParam MatchOutcome outcome) {
        return predictionService.savePrediction(username, code, matchId, outcome);
    }
    @GetMapping("/group-table")
    public List<GroupTableRow> getGroupTable(@RequestParam String username,
                                             @RequestParam String code,
                                             @RequestParam String group) {
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
    @GetMapping("/prediction/auto-group")
    public List<Prediction> autoPredictGroup(@RequestParam String username,
                                             @RequestParam String code,
                                             @RequestParam String group) {
        return predictionService.autoPredictGroup(username, code, group);
    }
    @GetMapping("/third-place-selection")
    public ThirdPlaceSelectionDto getThirdPlaceSelection(@RequestParam String username,
                                                         @RequestParam String code) {
        return predictionService.getThirdPlaceSelection(username, code);
    }
    @GetMapping("/third-place-selection/save")
    public List<String> saveQualifiedThirdPlaceTeams(@RequestParam String username,
                                                     @RequestParam String code,
                                                     @RequestParam List<String> teams) {
        return predictionService.saveQualifiedThirdPlaceTeams(username, code, teams);
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
    public List<KnockoutMatchDto> getRoundOf32(@RequestParam String username,
                                               @RequestParam String code) {
        return predictionService.buildRoundOf32(username, code);
    }
    @GetMapping("/knockout/save")
    public KnockoutPrediction saveKnockoutPrediction(@RequestParam String username,
                                                     @RequestParam String code,
                                                     @RequestParam String round,
                                                     @RequestParam int matchNumber,
                                                     @RequestParam String slot,
                                                     @RequestParam String winner) {
        return predictionService.saveKnockoutPrediction(username, code, round, matchNumber, slot, winner);
    }
    @GetMapping("/knockout/auto-round-of-32")
    public List<KnockoutPrediction> autoPickRoundOf32HomeTeams(@RequestParam String username,
                                                               @RequestParam String code) {
        return predictionService.autoPickRoundOf32HomeTeams(username, code);
    }
    @GetMapping("/round-of-16")
    public List<KnockoutMatchDto> getRoundOf16(@RequestParam String username,
                                               @RequestParam String code) {
        return predictionService.buildRoundOf16(username, code);
    }
    @GetMapping("/knockout/auto-round")
    public List<KnockoutPrediction> autoPickRound(@RequestParam String username,
                                                  @RequestParam String code,
                                                  @RequestParam String round) {
        return predictionService.autoPickRound(username, code, round);
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
}