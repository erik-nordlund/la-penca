package com.penca.lapenca.service;

import com.penca.lapenca.dto.*;
import com.penca.lapenca.entity.*;
import com.penca.lapenca.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final AppUserRepository appUserRepository;
    private final PartyRepository partyRepository;
    private final MatchRepository matchRepository;
    private final QualifiedThirdPlaceSelectionRepository qualifiedThirdPlaceSelectionRepository;
    private final TeamRepository teamRepository;
    private final KnockoutPredictionRepository knockoutPredictionRepository;
    private final ActualQualifiedTeamRepository actualQualifiedTeamRepository;
    private final PartyMemberRepository partyMemberRepository;


    public PredictionService(PredictionRepository predictionRepository,
                             AppUserRepository appUserRepository,
                             PartyRepository partyRepository,
                             MatchRepository matchRepository,
                             QualifiedThirdPlaceSelectionRepository qualifiedThirdPlaceSelectionRepository,
                             TeamRepository teamRepository, KnockoutPredictionRepository knockoutPredictionRepository,
                             ActualQualifiedTeamRepository actualQualifiedTeamRepository,
                             PartyMemberRepository partyMemberRepository) {
        this.predictionRepository = predictionRepository;
        this.appUserRepository = appUserRepository;
        this.partyRepository = partyRepository;
        this.matchRepository = matchRepository;
        this.qualifiedThirdPlaceSelectionRepository = qualifiedThirdPlaceSelectionRepository;
        this.teamRepository = teamRepository;
        this.knockoutPredictionRepository = knockoutPredictionRepository;
        this.actualQualifiedTeamRepository = actualQualifiedTeamRepository;
        this.partyMemberRepository = partyMemberRepository;
    }

    @Transactional
    public Prediction savePrediction(String username,
                                     String partyCode,
                                     Long matchId,
                                     MatchOutcome predictedOutcome) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        validatePartyNotLocked(party);

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

        boolean changed = prediction.getPredictedOutcome() != predictedOutcome;

        prediction.setPredictedOutcome(predictedOutcome);

        if (changed) {
            clearBracketAfterGroupChange(user, party);
        }

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
    public GroupResultDto getGroupResult(String username, String partyCode, String groupName) {
        List<GroupTableRow> table = calculateGroupTable(username, partyCode, groupName);

        if (table.size() < 3) {
            throw new RuntimeException("Not enough teams in group " + groupName);
        }

        return new GroupResultDto(
                groupName,
                table.get(0).getTeamName(),
                table.get(1).getTeamName(),
                table.get(2).getTeamName()
        );
    }
    public List<GroupResultDto> getAllGroupResults(String username, String partyCode) {
        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

        return groups.stream()
                .map(group -> getGroupResult(username, partyCode, group))
                .toList();
    }
    public ThirdPlaceTeamDto getThirdPlaceTeam(String username, String partyCode, String groupName) {
        List<GroupTableRow> table = calculateGroupTable(username, partyCode, groupName);

        if (table.size() < 3) {
            throw new RuntimeException("Not enough teams in group " + groupName);
        }

        GroupTableRow third = table.get(2);

        return new ThirdPlaceTeamDto(
                groupName,
                third.getTeamName(),
                third.getPoints()
        );
    }
    public List<ThirdPlaceTeamDto> getAllThirdPlaceTeams(String username, String partyCode) {
        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

        return groups.stream()
                .map(group -> getThirdPlaceTeam(username, partyCode, group))
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .toList();
    }
    public List<Prediction> autoPredictGroup(String username, String partyCode, String groupName) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));
        validatePartyNotLocked(party);

        List<Match> matches = matchRepository.findByStageAndHomeTeam_GroupName("GROUP", groupName);

        List<MatchOutcome> pattern = List.of(
                MatchOutcome.HOME_WIN,
                MatchOutcome.DRAW,
                MatchOutcome.AWAY_WIN
        );

        for (int i = 0; i < matches.size(); i++) {
            Match match = matches.get(i);
            MatchOutcome outcome = pattern.get(i % pattern.size());

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

            prediction.setPredictedOutcome(outcome);
            predictionRepository.save(prediction);
        }

        return predictionRepository.findByUserAndParty(user, party);
    }
    public ThirdPlaceSelectionDto getThirdPlaceSelection(String username, String partyCode) {
        List<ThirdPlaceTeamDto> thirdPlaceTeams = getAllThirdPlaceTeams(username, partyCode);

        int slots = 8;

        if (thirdPlaceTeams.size() <= slots) {
            return new ThirdPlaceSelectionDto(thirdPlaceTeams, List.of(), 0);
        }

        int cutoffPoints = thirdPlaceTeams.get(slots - 1).getPoints();

        List<ThirdPlaceTeamDto> automaticallyQualified = thirdPlaceTeams.stream()
                .filter(team -> team.getPoints() > cutoffPoints)
                .toList();

        List<ThirdPlaceTeamDto> tiedTeams = thirdPlaceTeams.stream()
                .filter(team -> team.getPoints() == cutoffPoints)
                .toList();

        int remainingSlots = slots - automaticallyQualified.size();

        return new ThirdPlaceSelectionDto(automaticallyQualified, tiedTeams, remainingSlots);
    }
    @Transactional
    public List<String> saveQualifiedThirdPlaceTeams(String username, String partyCode, List<String> teamNames) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        validatePartyNotLocked(party);

        if (teamNames.size() != 8) {
            throw new RuntimeException("You must select exactly 8 third-place teams");
        }

        qualifiedThirdPlaceSelectionRepository.deleteByUserAndParty(user, party);

        for (String teamName : teamNames) {
            Team team = teamRepository.findByName(teamName)
                    .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));

            QualifiedThirdPlaceSelection selection = QualifiedThirdPlaceSelection.builder()
                    .user(user)
                    .party(party)
                    .team(team)
                    .build();

            qualifiedThirdPlaceSelectionRepository.save(selection);
        }
        knockoutPredictionRepository.deleteByUserAndParty(user, party);
        return teamNames;
    }
    public QualifiedTeamsOverviewDto getQualifiedTeamsOverview(String username, String partyCode) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<GroupResultDto> groupResults = getAllGroupResults(username, partyCode);

        List<String> groupWinners = groupResults.stream()
                .map(GroupResultDto::getFirstPlace)
                .toList();

        List<String> groupRunnersUp = groupResults.stream()
                .map(GroupResultDto::getSecondPlace)
                .toList();

        List<String> qualifiedThirdPlaceTeams = qualifiedThirdPlaceSelectionRepository.findByUserAndParty(user, party)
                .stream()
                .map(selection -> selection.getTeam().getName())
                .toList();

        return new QualifiedTeamsOverviewDto(
                groupWinners,
                groupRunnersUp,
                qualifiedThirdPlaceTeams
        );
    }
    public List<ThirdPlaceRule> loadRules() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("data/third_place_rules.json");

            return mapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<ThirdPlaceRule>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to load rules", e);
        }
    }
    public ThirdPlaceRule findMatchingThirdPlaceRule(String username, String partyCode) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(partyCode)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<String> selectedGroups = qualifiedThirdPlaceSelectionRepository.findByUserAndParty(user, party)
                .stream()
                .map(selection -> selection.getTeam().getGroupName())
                .sorted()
                .toList();

        List<ThirdPlaceRule> rules = loadRules();

        return rules.stream()
                .filter(rule -> rule.getQualifiedGroups().stream().sorted().toList().equals(selectedGroups))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No matching third-place rule found for groups: " + selectedGroups));
    }
    public List<KnockoutMatchDto> buildRoundOf32(String username, String code) {
        QualifiedTeamsOverviewDto overview = getQualifiedTeamsOverview(username, code);
        ThirdPlaceRule rule = findMatchingThirdPlaceRule(username, code);

        Map<String, String> groupWinners = new HashMap<>();
        Map<String, String> groupRunnersUp = new HashMap<>();

        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

        for (int i = 0; i < groups.size(); i++) {
            groupWinners.put(groups.get(i), overview.getGroupWinners().get(i));
            groupRunnersUp.put(groups.get(i), overview.getGroupRunnersUp().get(i));
        }

        Map<String, String> thirdPlaceTeams = new HashMap<>();

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        qualifiedThirdPlaceSelectionRepository.findByUserAndParty(user, party)
                .forEach(selection -> {
                    String groupName = selection.getTeam().getGroupName();
                    String teamName = selection.getTeam().getName();
                    thirdPlaceTeams.put(groupName, teamName);
                });

        List<KnockoutMatchDto> matches = new ArrayList<>();

        // Dynamic third-place matches from FIFA rule mapping
        for (Map.Entry<String, String> entry : rule.getMapping().entrySet()) {
            String winnerSlot = entry.getKey();      // example: "1A"
            String thirdSlot = entry.getValue();     // example: "3E"

            String winnerGroup = winnerSlot.substring(1); // A
            String thirdGroup = thirdSlot.substring(1);    // E

            String homeTeam = groupWinners.get(winnerGroup);
            String awayTeam = thirdPlaceTeams.get(thirdGroup);

            String slot = winnerSlot + " vs " + thirdSlot;
            matches.add(new KnockoutMatchDto(slot, homeTeam, awayTeam));
        }

        matches.add(new KnockoutMatchDto("2A vs 2B", groupRunnersUp.get("A"), groupRunnersUp.get("B")));
        matches.add(new KnockoutMatchDto("1F vs 2C", groupWinners.get("F"), groupRunnersUp.get("C")));
        matches.add(new KnockoutMatchDto("2K vs 2L", groupRunnersUp.get("K"), groupRunnersUp.get("L")));
        matches.add(new KnockoutMatchDto("1H vs 2J", groupWinners.get("H"), groupRunnersUp.get("J")));
        matches.add(new KnockoutMatchDto("1C vs 2F", groupWinners.get("C"), groupRunnersUp.get("F")));
        matches.add(new KnockoutMatchDto("2E vs 2I", groupRunnersUp.get("E"), groupRunnersUp.get("I")));
        matches.add(new KnockoutMatchDto("1J vs 2H", groupWinners.get("J"), groupRunnersUp.get("H")));
        matches.add(new KnockoutMatchDto("2D vs 2G", groupRunnersUp.get("D"), groupRunnersUp.get("G")));

        return matches;
    }
    @Transactional
    public KnockoutPrediction saveKnockoutPrediction(String username,
                                                     String code,
                                                     String roundName,
                                                     int matchNumber,
                                                     String slot,
                                                     String winnerTeamName) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        validatePartyNotLocked(party);

        Team winner = teamRepository.findByName(winnerTeamName)
                .orElseThrow(() -> new RuntimeException("Team not found: " + winnerTeamName));

        KnockoutPrediction prediction = knockoutPredictionRepository
                .findByUserAndPartyAndRoundNameAndMatchNumber(user, party, roundName, matchNumber)
                .orElse(
                        KnockoutPrediction.builder()
                                .user(user)
                                .party(party)
                                .roundName(roundName)
                                .matchNumber(matchNumber)
                                .slot(slot)
                                .build()
                );

        boolean changed = prediction.getPredictedWinner() == null
                || !prediction.getPredictedWinner().getName().equals(winnerTeamName);

        prediction.setSlot(slot);
        prediction.setPredictedWinner(winner);

        KnockoutPrediction saved = knockoutPredictionRepository.save(prediction);

        if (changed) {
            clearAffectedFutureRounds(user, party, roundName, matchNumber);
        }

        return saved;
    }
    private void clearAffectedFutureRounds(AppUser user, Party party, String roundName, int matchNumber) {

        switch (roundName) {
            case "ROUND_OF_32" -> {
                int r16Match = nextMatchNumber(matchNumber);
                int qfMatch = nextMatchNumber(r16Match);
                int sfMatch = nextMatchNumber(qfMatch);

                deleteKnockoutMatch(user, party, "ROUND_OF_16", r16Match);
                deleteKnockoutMatch(user, party, "QUARTER_FINAL", qfMatch);
                deleteKnockoutMatch(user, party, "SEMI_FINAL", sfMatch);

                deleteKnockoutMatch(user, party, "FINAL", 1);
                deleteKnockoutMatch(user, party, "THIRD_PLACE", 1);
            }

            case "ROUND_OF_16" -> {
                int qfMatch = nextMatchNumber(matchNumber);
                int sfMatch = nextMatchNumber(qfMatch);

                deleteKnockoutMatch(user, party, "QUARTER_FINAL", qfMatch);
                deleteKnockoutMatch(user, party, "SEMI_FINAL", sfMatch);

                deleteKnockoutMatch(user, party, "FINAL", 1);
                deleteKnockoutMatch(user, party, "THIRD_PLACE", 1);
            }

            case "QUARTER_FINAL" -> {
                int sfMatch = nextMatchNumber(matchNumber);

                deleteKnockoutMatch(user, party, "SEMI_FINAL", sfMatch);

                deleteKnockoutMatch(user, party, "FINAL", 1);
                deleteKnockoutMatch(user, party, "THIRD_PLACE", 1);
            }

            case "SEMI_FINAL" -> {
                deleteKnockoutMatch(user, party, "FINAL", 1);
                deleteKnockoutMatch(user, party, "THIRD_PLACE", 1);
            }

            case "FINAL" -> {
            }

            case "THIRD_PLACE" -> {
            }
        }
    }

    private int nextMatchNumber(int matchNumber) {
        return (matchNumber + 1) / 2;
    }

    private void deleteKnockoutMatch(AppUser user, Party party, String roundName, int matchNumber) {
        knockoutPredictionRepository.deleteByUserAndPartyAndRoundNameAndMatchNumber(
                user,
                party,
                roundName,
                matchNumber
        );
    }

    public List<KnockoutPrediction> autoPickRoundOf32HomeTeams(String username, String code) {
        List<KnockoutMatchDto> roundOf32Matches = buildRoundOf32(username, code);

        List<KnockoutPrediction> savedPredictions = new ArrayList<>();

        for (int i = 0; i < roundOf32Matches.size(); i++) {
            KnockoutMatchDto match = roundOf32Matches.get(i);

            KnockoutPrediction prediction = saveKnockoutPrediction(
                    username,
                    code,
                    "ROUND_OF_32",
                    i + 1,
                    match.getSlot(),
                    match.getHomeTeam()
            );

            savedPredictions.add(prediction);
        }

        return savedPredictions;
    }
    public List<KnockoutMatchDto> buildRoundOf16(String username, String code) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<KnockoutPrediction> roundOf32Winners =
                knockoutPredictionRepository.findByUserAndPartyAndRoundNameOrderByMatchNumberAsc(
                        user,
                        party,
                        "ROUND_OF_32"
                );

        if (roundOf32Winners.size() != 16) {
            throw new RuntimeException("You must pick winners for all 16 Round of 32 matches first");
        }

        List<KnockoutMatchDto> matches = new ArrayList<>();

        for (int i = 0; i < roundOf32Winners.size(); i += 2) {
            KnockoutPrediction first = roundOf32Winners.get(i);
            KnockoutPrediction second = roundOf32Winners.get(i + 1);

            int matchNumber = (i / 2) + 1;

            matches.add(new KnockoutMatchDto(
                    "Winner R32-" + first.getMatchNumber() + " vs Winner R32-" + second.getMatchNumber(),
                    first.getPredictedWinner().getName(),
                    second.getPredictedWinner().getName()
            ));
        }

        return matches;
    }
    private List<KnockoutMatchDto> buildNextRound(String username,
                                                  String code,
                                                  String previousRoundName,
                                                  String previousRoundShortName,
                                                  int expectedPreviousWinners) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<KnockoutPrediction> previousWinners =
                knockoutPredictionRepository.findByUserAndPartyAndRoundNameOrderByMatchNumberAsc(
                        user,
                        party,
                        previousRoundName
                );

        if (previousWinners.size() != expectedPreviousWinners) {
            throw new RuntimeException("You must pick winners for all " + expectedPreviousWinners + " matches in " + previousRoundName + " first");
        }

        List<KnockoutMatchDto> matches = new ArrayList<>();

        for (int i = 0; i < previousWinners.size(); i += 2) {
            KnockoutPrediction first = previousWinners.get(i);
            KnockoutPrediction second = previousWinners.get(i + 1);

            matches.add(new KnockoutMatchDto(
                    "Winner " + previousRoundShortName + "-" + first.getMatchNumber()
                            + " vs Winner " + previousRoundShortName + "-" + second.getMatchNumber(),
                    first.getPredictedWinner().getName(),
                    second.getPredictedWinner().getName()
            ));
        }

        return matches;
    }
    public List<KnockoutMatchDto> buildQuarterFinals(String username, String code) {
        return buildNextRound(username, code, "ROUND_OF_16", "R16", 8);
    }

    public List<KnockoutMatchDto> buildSemiFinals(String username, String code) {
        return buildNextRound(username, code, "QUARTER_FINAL", "QF", 4);
    }

    public List<KnockoutMatchDto> buildFinal(String username, String code) {
        return buildNextRound(username, code, "SEMI_FINAL", "SF", 2);
    }
    public List<KnockoutMatchDto> buildThirdPlaceMatch(String username, String code) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<KnockoutMatchDto> semiFinalMatches = buildSemiFinals(username, code);

        List<KnockoutPrediction> semiFinalPredictions =
                knockoutPredictionRepository.findByUserAndPartyAndRoundNameOrderByMatchNumberAsc(
                        user,
                        party,
                        "SEMI_FINAL"
                );

        if (semiFinalPredictions.size() != 2) {
            throw new RuntimeException("You must pick winners for both semi finals first");
        }

        List<String> bronzeTeams = new ArrayList<>();

        for (int i = 0; i < semiFinalMatches.size(); i++) {
            KnockoutMatchDto semiMatch = semiFinalMatches.get(i);
            KnockoutPrediction prediction = semiFinalPredictions.get(i);

            String winner = prediction.getPredictedWinner().getName();

            String loser = semiMatch.getHomeTeam().equals(winner)
                    ? semiMatch.getAwayTeam()
                    : semiMatch.getHomeTeam();

            bronzeTeams.add(loser);
        }

        return List.of(new KnockoutMatchDto(
                "Loser SF-1 vs Loser SF-2",
                bronzeTeams.get(0),
                bronzeTeams.get(1)
        ));
    }
    public List<KnockoutPrediction> autoPickRound(String username, String code, String roundName) {
        List<KnockoutMatchDto> matches;

        if (roundName.equals("ROUND_OF_16")) {
            matches = buildRoundOf16(username, code);
        } else if (roundName.equals("QUARTER_FINAL")) {
            matches = buildQuarterFinals(username, code);
        } else if (roundName.equals("SEMI_FINAL")) {
            matches = buildSemiFinals(username, code);
        } else if (roundName.equals("THIRD_PLACE")) {
            matches = buildThirdPlaceMatch(username, code);
        } else if (roundName.equals("FINAL")) {
            matches = buildFinal(username, code);
        } else {
            throw new RuntimeException("Unknown round: " + roundName);
        }

        List<KnockoutPrediction> saved = new ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            KnockoutMatchDto match = matches.get(i);

            saved.add(saveKnockoutPrediction(
                    username,
                    code,
                    roundName,
                    i + 1,
                    match.getSlot(),
                    match.getHomeTeam()
            ));
        }

        return saved;
    }
    public KnockoutBracketDto getFullBracket(String username, String code) {

        List<KnockoutMatchDto> r32 = buildRoundOf32(username, code);
        List<KnockoutMatchDto> r16 = buildRoundOf16(username, code);
        List<KnockoutMatchDto> qf = buildQuarterFinals(username, code);
        List<KnockoutMatchDto> sf = buildSemiFinals(username, code);
        List<KnockoutMatchDto> finalMatch = buildFinal(username, code);

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        String champion = knockoutPredictionRepository
                .findByUserAndPartyAndRoundNameAndMatchNumber(user, party, "FINAL", 1)
                .map(prediction -> prediction.getPredictedWinner().getName())
                .orElse(null);

        return new KnockoutBracketDto(
                r32,
                r16,
                qf,
                sf,
                finalMatch,
                champion
        );
    }
    public List<Prediction> getPredictions(String username, String code) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return predictionRepository.findByUserAndParty(user, party);
    }
    public List<KnockoutPrediction> getKnockoutPredictions(String username, String code) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return knockoutPredictionRepository.findByUserAndParty(user, party);
    }
    public List<ThirdPlaceTeamDto> getSavedThirdPlaceTeams(String username, String code) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return qualifiedThirdPlaceSelectionRepository
                .findByUserAndParty(user, party)
                .stream()
                .map(selection -> new ThirdPlaceTeamDto(
                        selection.getTeam().getGroupName(),
                        selection.getTeam().getName(),
                        0
                ))
                .toList();
    }
    public int calculateUserScore(String username, String code) {
        return calculateUserScoreBreakdown(username, code).getTotalPoints();
    }

    private int safeCountCorrectRoundOf32Teams(String username, String code) {
        try {
            return countCorrectRoundOf32Teams(username, code);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private int safeCalculatePlacementScore(String username, String code, AppUser user, Party party) {
        try {
            return calculatePlacementScore(username, code, user, party);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private int countCorrectRoundOf32Teams(String username, String code) {

        List<String> predictedTeams = buildRoundOf32(username, code)
                .stream()
                .flatMap(match -> List.of(match.getHomeTeam(), match.getAwayTeam()).stream())
                .toList();

        List<String> actualTeams = actualQualifiedTeamRepository.findByStage("ROUND_OF_32")
                .stream()
                .map(actual -> actual.getTeam().getName())
                .toList();

        int count = 0;

        for (String predictedTeam : predictedTeams) {
            if (actualTeams.contains(predictedTeam)) {
                count++;
            }
        }

        return count;
    }

    private int countCorrectKnockoutTeams(AppUser user,
                                          Party party,
                                          String predictionRound,
                                          String actualStage) {

        List<String> predictedTeams = knockoutPredictionRepository
                .findByUserAndPartyAndRoundName(user, party, predictionRound)
                .stream()
                .map(prediction -> prediction.getPredictedWinner().getName())
                .toList();

        List<String> actualTeams = actualQualifiedTeamRepository
                .findByStage(actualStage)
                .stream()
                .map(actual -> actual.getTeam().getName())
                .toList();

        int count = 0;

        for (String predictedTeam : predictedTeams) {
            if (actualTeams.contains(predictedTeam)) {
                count++;
            }
        }

        return count;
    }
    public ActualQualifiedTeam addActualQualifiedTeam(String teamName, String stage) {
        Team team = teamRepository.findByName(teamName)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        ActualQualifiedTeam actual = ActualQualifiedTeam.builder()
                .team(team)
                .stage(stage)
                .build();

        return actualQualifiedTeamRepository.save(actual);
    }
    public Match setActualMatchResult(Long matchId, int homeScore, int awayScore) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setPlayed(true);

        return matchRepository.save(match);
    }
    public List<LeaderboardRowDto> getLeaderboard(String code) {

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        List<PartyMember> members = partyMemberRepository.findByParty(party);

        return members.stream()
                .map(member -> {
                    String username = member.getUser().getUsername();
                    int score = calculateUserScore(username, code);

                    return new LeaderboardRowDto(username, score);
                })
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .toList();
    }
    private int calculatePlacementScore(String username, String code, AppUser user, Party party) {

        int score = 0;

        List<KnockoutPrediction> finalPredictions =
                knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "FINAL");

        if (!finalPredictions.isEmpty()) {
            KnockoutPrediction finalPrediction = finalPredictions.get(0);
            String predictedChampion = finalPrediction.getPredictedWinner().getName();

            List<KnockoutMatchDto> finalMatch = buildFinal(username, code);

            if (!finalMatch.isEmpty()) {
                KnockoutMatchDto match = finalMatch.get(0);

                String predictedRunnerUp = match.getHomeTeam().equals(predictedChampion)
                        ? match.getAwayTeam()
                        : match.getHomeTeam();

                List<String> actualRunnerUp = actualQualifiedTeamRepository.findByStage("RUNNER_UP")
                        .stream()
                        .map(actual -> actual.getTeam().getName())
                        .toList();

                if (actualRunnerUp.contains(predictedRunnerUp)) {
                    score += 5;
                }
            }
        }

        List<KnockoutPrediction> thirdPlacePredictions =
                knockoutPredictionRepository.findByUserAndPartyAndRoundName(user, party, "THIRD_PLACE");

        if (!thirdPlacePredictions.isEmpty()) {
            KnockoutPrediction thirdPlacePrediction = thirdPlacePredictions.get(0);
            String predictedThirdPlace = thirdPlacePrediction.getPredictedWinner().getName();

            List<String> actualThirdPlace = actualQualifiedTeamRepository.findByStage("THIRD_PLACE")
                    .stream()
                    .map(actual -> actual.getTeam().getName())
                    .toList();

            if (actualThirdPlace.contains(predictedThirdPlace)) {
                score += 4;
            }

            List<KnockoutMatchDto> thirdPlaceMatch = buildThirdPlaceMatch(username, code);

            if (!thirdPlaceMatch.isEmpty()) {
                KnockoutMatchDto match = thirdPlaceMatch.get(0);

                String predictedFourthPlace = match.getHomeTeam().equals(predictedThirdPlace)
                        ? match.getAwayTeam()
                        : match.getHomeTeam();

                List<String> actualFourthPlace = actualQualifiedTeamRepository.findByStage("FOURTH_PLACE")
                        .stream()
                        .map(actual -> actual.getTeam().getName())
                        .toList();

                if (actualFourthPlace.contains(predictedFourthPlace)) {
                    score += 3;
                }
            }
        }

        return score;
    }
    public String resetActualData() {

        actualQualifiedTeamRepository.deleteAll();

        List<Match> matches = matchRepository.findAll();

        for (Match match : matches) {
            match.setHomeScore(null);
            match.setAwayScore(null);
            match.setPlayed(false);
        }

        matchRepository.saveAll(matches);

        return "Actual data reset";
    }
    private void clearFutureRounds(AppUser user, Party party, String roundName) {

        List<String> roundsToDelete = new ArrayList<>();

        switch (roundName) {
            case "ROUND_OF_32" -> roundsToDelete = List.of(
                    "ROUND_OF_16", "QUARTER_FINAL", "SEMI_FINAL", "THIRD_PLACE", "FINAL"
            );
            case "ROUND_OF_16" -> roundsToDelete = List.of(
                    "QUARTER_FINAL", "SEMI_FINAL", "THIRD_PLACE", "FINAL"
            );
            case "QUARTER_FINAL" -> roundsToDelete = List.of(
                    "SEMI_FINAL", "THIRD_PLACE", "FINAL"
            );
            case "SEMI_FINAL" -> roundsToDelete = List.of(
                    "THIRD_PLACE", "FINAL"
            );
            case "THIRD_PLACE" -> roundsToDelete = List.of();
            case "FINAL" -> roundsToDelete = List.of();
        }

        for (String round : roundsToDelete) {
            knockoutPredictionRepository.deleteByUserAndPartyAndRoundName(user, party, round);
        }
    }
    private void clearBracketAfterGroupChange(AppUser user, Party party) {
        qualifiedThirdPlaceSelectionRepository.deleteByUserAndParty(user, party);
        knockoutPredictionRepository.deleteByUserAndParty(user, party);
    }
    public ScoreBreakdownDto calculateUserScoreBreakdown(String username, String code) {

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        int groupPoints = calculateGroupPoints(user, party);

        int roundOf32Points = safeCountCorrectRoundOf32Teams(username, code) * 1;

        int roundOf16Points = countCorrectKnockoutTeams(user, party, "ROUND_OF_32", "ROUND_OF_16") * 2;
        int quarterFinalPoints = countCorrectKnockoutTeams(user, party, "ROUND_OF_16", "QUARTER_FINAL") * 3;
        int semiFinalPoints = countCorrectKnockoutTeams(user, party, "QUARTER_FINAL", "SEMI_FINAL") * 4;
        int finalPoints = countCorrectKnockoutTeams(user, party, "SEMI_FINAL", "FINAL") * 5;

        int championPoints = countCorrectKnockoutTeams(user, party, "FINAL", "CHAMPION") * 6;

        int placementPoints = safeCalculatePlacementScore(username, code, user, party);

        return new ScoreBreakdownDto(
                username,
                groupPoints,
                roundOf32Points,
                roundOf16Points,
                quarterFinalPoints,
                semiFinalPoints,
                finalPoints + championPoints,
                placementPoints
        );
    }
    private int calculateGroupPoints(AppUser user, Party party) {
        int points = 0;

        List<Prediction> groupPredictions = predictionRepository.findByUserAndParty(user, party);

        for (Prediction prediction : groupPredictions) {
            Match match = prediction.getMatch();

            if (!"GROUP".equals(match.getStage())) {
                continue;
            }

            if (!match.isPlayed() || match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }

            MatchOutcome actualOutcome;

            if (match.getHomeScore() > match.getAwayScore()) {
                actualOutcome = MatchOutcome.HOME_WIN;
            } else if (match.getHomeScore() < match.getAwayScore()) {
                actualOutcome = MatchOutcome.AWAY_WIN;
            } else {
                actualOutcome = MatchOutcome.DRAW;
            }

            if (prediction.getPredictedOutcome() == actualOutcome) {
                points += 1;
            }
        }

        return points;
    }
    private void validatePartyNotLocked(Party party) {
        if (party.getPredictionDeadline() != null &&
                LocalDateTime.now().isAfter(party.getPredictionDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Predictions are locked for this party"
            );
        }
    }
}