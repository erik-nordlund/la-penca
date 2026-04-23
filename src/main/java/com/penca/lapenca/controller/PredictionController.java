package com.penca.lapenca.controller;

import com.penca.lapenca.dto.GroupTableRow;
import com.penca.lapenca.dto.QualifiedTeamsDto;
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
}