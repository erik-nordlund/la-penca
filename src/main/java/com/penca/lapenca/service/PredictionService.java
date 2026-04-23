package com.penca.lapenca.service;

import com.penca.lapenca.dto.QualifiedTeamsDto;
import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.AppUserRepository;
import com.penca.lapenca.repository.MatchRepository;
import com.penca.lapenca.repository.PartyRepository;
import com.penca.lapenca.repository.PredictionRepository;
import org.springframework.stereotype.Service;
import com.penca.lapenca.dto.GroupTableRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final AppUserRepository appUserRepository;
    private final PartyRepository partyRepository;
    private final MatchRepository matchRepository;

    public PredictionService(PredictionRepository predictionRepository,
                             AppUserRepository appUserRepository,
                             PartyRepository partyRepository,
                             MatchRepository matchRepository) {
        this.predictionRepository = predictionRepository;
        this.appUserRepository = appUserRepository;
        this.partyRepository = partyRepository;
        this.matchRepository = matchRepository;
    }

    public Prediction savePrediction(String username,
                                     String partyCode,
                                     Long matchId,
                                     MatchOutcome predictedOutcome) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Prediction prediction = predictionRepository
                .findByUserAndPartyAndMatch(user, party, match)
                .orElse(
                        Prediction.builder()
                                .user(user)
                                .party(party)
                                .match(match)
                                .pointsAwarded(0)
                                .build()
                );

        prediction.setPredictedOutcome(predictedOutcome);

        return predictionRepository.save(prediction);
    }

    public List<GroupTableRow> calculateGroupTable(String username, String partyCode, String groupName) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<Prediction> predictions = predictionRepository.findByUserAndParty(user, party);

        Map<String, GroupTableRow> table = new HashMap<>();

        for (Prediction prediction : predictions) {
            Match match = prediction.getMatch();

            if (!"GROUP".equals(match.getStage())) {
                continue;
            }

            String homeGroup = match.getHomeTeam().getGroupName();
            String awayGroup = match.getAwayTeam().getGroupName();

            if (!groupName.equals(homeGroup) || !groupName.equals(awayGroup)) {
                continue;
            }

            String homeTeamName = match.getHomeTeam().getName();
            String awayTeamName = match.getAwayTeam().getName();

            table.putIfAbsent(homeTeamName, new GroupTableRow(homeTeamName, 0, 0, 0, 0, 0));
            table.putIfAbsent(awayTeamName, new GroupTableRow(awayTeamName, 0, 0, 0, 0, 0));

            GroupTableRow homeRow = table.get(homeTeamName);
            GroupTableRow awayRow = table.get(awayTeamName);

            homeRow.setPlayed(homeRow.getPlayed() + 1);
            awayRow.setPlayed(awayRow.getPlayed() + 1);

            MatchOutcome outcome = prediction.getPredictedOutcome();

            if (outcome == MatchOutcome.HOME_WIN) {
                homeRow.setWins(homeRow.getWins() + 1);
                homeRow.setPoints(homeRow.getPoints() + 3);

                awayRow.setLosses(awayRow.getLosses() + 1);
            } else if (outcome == MatchOutcome.AWAY_WIN) {
                awayRow.setWins(awayRow.getWins() + 1);
                awayRow.setPoints(awayRow.getPoints() + 3);

                homeRow.setLosses(homeRow.getLosses() + 1);
            } else if (outcome == MatchOutcome.DRAW) {
                homeRow.setDraws(homeRow.getDraws() + 1);
                awayRow.setDraws(awayRow.getDraws() + 1);

                homeRow.setPoints(homeRow.getPoints() + 1);
                awayRow.setPoints(awayRow.getPoints() + 1);
            }
        }

        return table.values().stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .toList();
    }

    public QualifiedTeamsDto getQualifiedTeams(String username, String partyCode, String groupName) {
        List<GroupTableRow> table = calculateGroupTable(username, partyCode, groupName);

        if (table.size() < 2) {
            throw new RuntimeException("Not enough teams in group table");
        }

        return new QualifiedTeamsDto(
                table.get(0).getTeamName(),
                table.get(1).getTeamName()
        );
    }
}