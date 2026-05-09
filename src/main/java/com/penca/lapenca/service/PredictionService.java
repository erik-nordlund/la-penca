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
    private final ActualKnockoutResultRepository actualKnockoutResultRepository;
    private final GroupTieBreakRankingRepository groupTieBreakRankingRepository;


    public PredictionService(PredictionRepository predictionRepository,
                             AppUserRepository appUserRepository,
                             PartyRepository partyRepository,
                             MatchRepository matchRepository,
                             QualifiedThirdPlaceSelectionRepository qualifiedThirdPlaceSelectionRepository,
                             TeamRepository teamRepository, KnockoutPredictionRepository knockoutPredictionRepository,
                             ActualQualifiedTeamRepository actualQualifiedTeamRepository,
                             PartyMemberRepository partyMemberRepository,
                             ActualKnockoutResultRepository actualKnockoutResultRepository,
                             GroupTieBreakRankingRepository groupTieBreakRankingRepository) {
        this.predictionRepository = predictionRepository;
        this.appUserRepository = appUserRepository;
        this.partyRepository = partyRepository;
        this.matchRepository = matchRepository;
        this.qualifiedThirdPlaceSelectionRepository = qualifiedThirdPlaceSelectionRepository;
        this.teamRepository = teamRepository;
        this.knockoutPredictionRepository = knockoutPredictionRepository;
        this.actualQualifiedTeamRepository = actualQualifiedTeamRepository;
        this.partyMemberRepository = partyMemberRepository;
        this.actualKnockoutResultRepository = actualKnockoutResultRepository;
        this.groupTieBreakRankingRepository = groupTieBreakRankingRepository;
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

        List<GroupTableRow> rows = new ArrayList<>(table.values());

        List<GroupTieBreakRanking> savedRanking =
                groupTieBreakRankingRepository.findByUserAndPartyAndGroupNameOrderByPositionIndexAsc(
                        user,
                        party,
                        groupName
                );

        if (!savedRanking.isEmpty()) {
            Map<String, Integer> rankingMap = new HashMap<>();

            for (GroupTieBreakRanking ranking : savedRanking) {
                rankingMap.put(ranking.getTeam().getName(), ranking.getPositionIndex());
            }

            rows.sort((a, b) -> {
                int pointsCompare = Integer.compare(b.getPoints(), a.getPoints());

                if (pointsCompare != 0) {
                    return pointsCompare;
                }

                Integer aRank = rankingMap.get(a.getTeamName());
                Integer bRank = rankingMap.get(b.getTeamName());

                if (aRank != null && bRank != null) {
                    return Integer.compare(aRank, bRank);
                }

                return a.getTeamName().compareTo(b.getTeamName());
            });

            return rows;
        }

        return rows.stream()
                .sorted((a, b) -> {
                    int pointsCompare = Integer.compare(b.getPoints(), a.getPoints());

                    if (pointsCompare != 0) {
                        return pointsCompare;
                    }

                    return a.getTeamName().compareTo(b.getTeamName());
                })
                .toList();
    }
    @Transactional
    public List<GroupTieBreakRanking> saveGroupTieBreakRanking(String username,
                                                               String code,
                                                               String groupName,
                                                               List<String> teamNames) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        validatePartyNotLocked(party);

        if (teamNames.size() != 4) {
            throw new RuntimeException("You must rank exactly 4 teams");
        }

        groupTieBreakRankingRepository.deleteByUserAndPartyAndGroupName(user, party, groupName);

        List<GroupTieBreakRanking> saved = new ArrayList<>();

        for (int i = 0; i < teamNames.size(); i++) {
            int position = i + 1;
            String teamName = teamNames.get(i);

            Team team = teamRepository.findByName(teamName)
                    .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));

            GroupTieBreakRanking ranking = GroupTieBreakRanking.builder()
                    .user(user)
                    .party(party)
                    .groupName(groupName)
                    .team(team)
                    .positionIndex(position)
                    .build();

            saved.add(groupTieBreakRankingRepository.save(ranking));
        }

        clearBracketAfterGroupChange(user, party);

        return saved;
    }

    public List<String> getGroupTieBreakRanking(String username,
                                                String code,
                                                String groupName) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return groupTieBreakRankingRepository
                .findByUserAndPartyAndGroupNameOrderByPositionIndexAsc(user, party, groupName)
                .stream()
                .map(ranking -> ranking.getTeam().getName())
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
    private record R32TemplateMatch(String homeSlot, String awaySlot, String awayMappingKey) {}

    private List<R32TemplateMatch> getRoundOf32Template() {
        return List.of(
                // LEFT SIDE — top to bottom
                new R32TemplateMatch("1E", null, "1E"),
                new R32TemplateMatch("1I", null, "1I"),
                new R32TemplateMatch("2A", "2B", null),
                new R32TemplateMatch("1F", "2C", null),

                new R32TemplateMatch("2K", "2L", null),
                new R32TemplateMatch("1H", "2J", null),
                new R32TemplateMatch("1D", null, "1D"),
                new R32TemplateMatch("1G", null, "1G"),

                // RIGHT SIDE — top to bottom
                new R32TemplateMatch("1C", "2F", null),
                new R32TemplateMatch("2E", "2I", null),
                new R32TemplateMatch("1A", null, "1A"),
                new R32TemplateMatch("1L", null, "1L"),

                new R32TemplateMatch("1J", "2H", null),
                new R32TemplateMatch("2D", "2G", null),
                new R32TemplateMatch("1B", null, "1B"),
                new R32TemplateMatch("1K", null, "1K")
        );
    }

    private String resolveKnockoutSlot(String slot,
                                       Map<String, String> groupWinners,
                                       Map<String, String> groupRunnersUp,
                                       Map<String, String> thirdPlaceTeams) {
        int position = Integer.parseInt(slot.substring(0, 1));
        String group = slot.substring(1);

        return switch (position) {
            case 1 -> groupWinners.get(group);
            case 2 -> groupRunnersUp.get(group);
            case 3 -> thirdPlaceTeams.get(group);
            default -> throw new RuntimeException("Unknown knockout slot: " + slot);
        };
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

        for (R32TemplateMatch template : getRoundOf32Template()) {
            String homeSlot = template.homeSlot();

            String awaySlot = template.awaySlot();
            if (awaySlot == null) {
                awaySlot = rule.getMapping().get(template.awayMappingKey());
            }

            String homeTeam = resolveKnockoutSlot(
                    homeSlot,
                    groupWinners,
                    groupRunnersUp,
                    thirdPlaceTeams
            );

            String awayTeam = resolveKnockoutSlot(
                    awaySlot,
                    groupWinners,
                    groupRunnersUp,
                    thirdPlaceTeams
            );

            matches.add(new KnockoutMatchDto(
                    homeSlot + " vs " + awaySlot,
                    homeTeam,
                    awayTeam
            ));
        }

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
    public List<Prediction> getPredictions(String viewerUsername, String targetUsername, String code) {
        validateCanViewPredictions(viewerUsername, targetUsername, code);

        AppUser targetUser = appUserRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return predictionRepository.findByUserAndParty(targetUser, party);
    }

    public List<KnockoutPrediction> getKnockoutPredictions(String viewerUsername, String targetUsername, String code) {
        validateCanViewPredictions(viewerUsername, targetUsername, code);

        AppUser targetUser = appUserRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        return knockoutPredictionRepository.findByUserAndParty(targetUser, party);
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

        if (actualQualifiedTeamRepository.existsByTeamAndStage(team, stage)) {
            throw new RuntimeException("Team already exists in stage: " + stage);
        }

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
    @Transactional
    public String resetActualData() {
        actualKnockoutResultRepository.deleteAll();
        actualQualifiedTeamRepository.deleteAll();

        List<Match> matches = matchRepository.findAll();

        for (Match match : matches) {
            match.setHomeScore(null);
            match.setAwayScore(null);
            match.setPlayed(false);
        }

        matchRepository.saveAll(matches);

        actualKnockoutResultRepository.flush();
        actualQualifiedTeamRepository.flush();

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
    public void validateCanViewPredictions(String viewerUsername, String targetUsername, String code) {
        AppUser viewer = appUserRepository.findByUsername(viewerUsername)
                .orElseThrow(() -> new RuntimeException("Viewer not found"));

        AppUser targetUser = appUserRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Party party = partyRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Party not found"));

        if (viewer.getId().equals(targetUser.getId())) {
            return;
        }

        if (party.getPredictionDeadline() == null ||
                !LocalDateTime.now().isAfter(party.getPredictionDeadline())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view other users' predictions after the party deadline"
            );
        }
    }
    public Match resetActualMatchResult(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setHomeScore(null);
        match.setAwayScore(null);
        match.setPlayed(false);

        return matchRepository.save(match);
    }

    public List<Match> getGroupMatches(String group) {
        return matchRepository.findByStageAndHomeTeam_GroupNameOrderByMatchDateAsc("GROUP", group);
    }
    public String resetActualGroupResults(String group) {
        List<Match> matches = matchRepository.findByStageAndHomeTeam_GroupNameOrderByMatchDateAsc("GROUP", group);

        for (Match match : matches) {
            match.setHomeScore(null);
            match.setAwayScore(null);
            match.setPlayed(false);
        }

        matchRepository.saveAll(matches);

        return "Group " + group + " results reset";
    }
    public List<GroupTableRow> calculateActualGroupTable(String groupName) {
        List<Match> matches = matchRepository
                .findByStageAndHomeTeam_GroupNameOrderByMatchDateAsc("GROUP", groupName);

        Map<String, GroupTableRow> table = new HashMap<>();

        for (Match match : matches) {
            String homeTeamName = match.getHomeTeam().getName();
            String awayTeamName = match.getAwayTeam().getName();

            table.putIfAbsent(homeTeamName, new GroupTableRow(homeTeamName, 0, 0, 0, 0, 0));
            table.putIfAbsent(awayTeamName, new GroupTableRow(awayTeamName, 0, 0, 0, 0, 0));

            if (!match.isPlayed() || match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }

            GroupTableRow homeRow = table.get(homeTeamName);
            GroupTableRow awayRow = table.get(awayTeamName);

            homeRow.setPlayed(homeRow.getPlayed() + 1);
            awayRow.setPlayed(awayRow.getPlayed() + 1);

            if (match.getHomeScore() > match.getAwayScore()) {
                homeRow.setWins(homeRow.getWins() + 1);
                homeRow.setPoints(homeRow.getPoints() + 3);
                awayRow.setLosses(awayRow.getLosses() + 1);
            } else if (match.getHomeScore() < match.getAwayScore()) {
                awayRow.setWins(awayRow.getWins() + 1);
                awayRow.setPoints(awayRow.getPoints() + 3);
                homeRow.setLosses(homeRow.getLosses() + 1);
            } else {
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

    public List<ThirdPlaceTeamDto> getActualThirdPlaceTeams() {
        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

        return groups.stream()
                .map(group -> {
                    List<GroupTableRow> table = calculateActualGroupTable(group);

                    if (table.size() < 3) {
                        throw new RuntimeException("Not enough teams in group " + group);
                    }

                    GroupTableRow third = table.get(2);

                    return new ThirdPlaceTeamDto(
                            group,
                            third.getTeamName(),
                            third.getPoints()
                    );
                })
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .toList();
    }

    @Transactional
    public List<ActualQualifiedTeam> saveActualRoundOf32Teams(List<String> thirdPlaceTeamNames) {
        if (thirdPlaceTeamNames.size() != 8) {
            throw new RuntimeException("You must select exactly 8 third-place teams");
        }

        List<String> previousRoundOf32Teams = actualQualifiedTeamRepository.findByStage("ROUND_OF_32")
                .stream()
                .map(actual -> actual.getTeam().getName())
                .sorted()
                .toList();

        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
        List<String> allQualifiedTeamNames = new ArrayList<>();

        for (String group : groups) {
            List<GroupTableRow> table = calculateActualGroupTable(group);

            if (table.size() < 2) {
                throw new RuntimeException("Not enough teams in group " + group);
            }

            allQualifiedTeamNames.add(table.get(0).getTeamName());
            allQualifiedTeamNames.add(table.get(1).getTeamName());
        }

        allQualifiedTeamNames.addAll(thirdPlaceTeamNames);

        List<String> newRoundOf32Teams = allQualifiedTeamNames.stream()
                .sorted()
                .toList();

        boolean changed = !previousRoundOf32Teams.equals(newRoundOf32Teams);

        if (changed) {
            clearAllActualKnockoutDataAfterRoundOf32Change();
        }

        actualQualifiedTeamRepository.deleteByStage("ROUND_OF_32");
        actualQualifiedTeamRepository.flush();

        List<ActualQualifiedTeam> saved = new ArrayList<>();

        for (String teamName : allQualifiedTeamNames) {
            Team team = teamRepository.findByName(teamName)
                    .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));

            ActualQualifiedTeam actual = ActualQualifiedTeam.builder()
                    .team(team)
                    .stage("ROUND_OF_32")
                    .build();

            saved.add(actualQualifiedTeamRepository.save(actual));
        }

        return saved;
    }
    private void clearAllActualKnockoutDataAfterRoundOf32Change() {
        List<String> knockoutRounds = List.of(
                "ROUND_OF_32",
                "ROUND_OF_16",
                "QUARTER_FINAL",
                "SEMI_FINAL",
                "FINAL",
                "THIRD_PLACE"
        );

        for (String round : knockoutRounds) {
            actualKnockoutResultRepository.deleteByRoundName(round);
        }

        List<String> futureStages = List.of(
                "ROUND_OF_16",
                "QUARTER_FINAL",
                "SEMI_FINAL",
                "FINAL",
                "CHAMPION",
                "RUNNER_UP",
                "THIRD_PLACE",
                "FOURTH_PLACE"
        );

        for (String stage : futureStages) {
            actualQualifiedTeamRepository.deleteByStage(stage);
        }

        actualKnockoutResultRepository.flush();
        actualQualifiedTeamRepository.flush();
    }

    public List<ActualQualifiedTeam> getActualQualifiedTeamsByStage(String stage) {
        return actualQualifiedTeamRepository.findByStage(stage);
    }

    public String resetActualQualifiedTeamsByStage(String stage) {
        actualQualifiedTeamRepository.deleteByStage(stage);
        return stage + " actual teams reset";
    }

    public String deleteActualQualifiedTeam(Long id) {
        actualQualifiedTeamRepository.deleteById(id);
        return "Actual qualified team deleted";
    }
    public List<KnockoutMatchDto> buildActualRoundOf32() {
        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

        Map<String, String> groupWinners = new HashMap<>();
        Map<String, String> groupRunnersUp = new HashMap<>();
        Map<String, String> thirdPlaceTeams = new HashMap<>();

        List<ActualQualifiedTeam> actualRoundOf32Teams =
                actualQualifiedTeamRepository.findByStage("ROUND_OF_32");

        if (actualRoundOf32Teams.size() < 32) {
            throw new RuntimeException("You must save 32 actual Round of 32 teams first");
        }

        for (String group : groups) {
            List<GroupTableRow> table = calculateActualGroupTable(group);

            if (table.size() < 3) {
                throw new RuntimeException("Group " + group + " does not have enough teams");
            }

            groupWinners.put(group, table.get(0).getTeamName());
            groupRunnersUp.put(group, table.get(1).getTeamName());

            String thirdTeamName = table.get(2).getTeamName();

            boolean thirdQualified = actualRoundOf32Teams.stream()
                    .anyMatch(actual -> actual.getTeam().getName().equals(thirdTeamName));

            if (thirdQualified) {
                thirdPlaceTeams.put(group, thirdTeamName);
            }
        }

        List<String> qualifiedThirdGroups = thirdPlaceTeams.keySet()
                .stream()
                .sorted()
                .toList();

        ThirdPlaceRule rule = loadRules()
                .stream()
                .filter(r -> r.getQualifiedGroups()
                        .stream()
                        .sorted()
                        .toList()
                        .equals(qualifiedThirdGroups))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No third-place rule found for groups: " + qualifiedThirdGroups));

        List<KnockoutMatchDto> matches = new ArrayList<>();

        for (R32TemplateMatch template : getRoundOf32Template()) {
            String homeSlot = template.homeSlot();

            String awaySlot = template.awaySlot();
            if (awaySlot == null) {
                awaySlot = rule.getMapping().get(template.awayMappingKey());
            }

            String homeTeam = resolveKnockoutSlot(
                    homeSlot,
                    groupWinners,
                    groupRunnersUp,
                    thirdPlaceTeams
            );

            String awayTeam = resolveKnockoutSlot(
                    awaySlot,
                    groupWinners,
                    groupRunnersUp,
                    thirdPlaceTeams
            );

            matches.add(new KnockoutMatchDto(
                    homeSlot + " vs " + awaySlot,
                    homeTeam,
                    awayTeam
            ));
        }

        return matches;
    }

    public List<KnockoutMatchDto> buildActualNextRound(String previousRoundName, String previousShortName, int expectedWinners) {
        List<ActualKnockoutResult> previousResults =
                actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc(previousRoundName);

        List<ActualKnockoutResult> playedResults = previousResults.stream()
                .filter(result -> result.isPlayed() && result.getWinner() != null)
                .toList();

        if (playedResults.size() != expectedWinners) {
            throw new RuntimeException("You must save all results for " + previousRoundName + " first");
        }

        List<KnockoutMatchDto> matches = new ArrayList<>();

        for (int i = 0; i < playedResults.size(); i += 2) {
            ActualKnockoutResult first = playedResults.get(i);
            ActualKnockoutResult second = playedResults.get(i + 1);

            matches.add(new KnockoutMatchDto(
                    "Winner " + previousShortName + "-" + first.getMatchNumber()
                            + " vs Winner " + previousShortName + "-" + second.getMatchNumber(),
                    first.getWinner().getName(),
                    second.getWinner().getName()
            ));
        }

        return matches;
    }

    public List<KnockoutMatchDto> buildActualKnockoutRound(String roundName) {
        return switch (roundName) {
            case "ROUND_OF_32" -> buildActualRoundOf32();
            case "ROUND_OF_16" -> buildActualNextRound("ROUND_OF_32", "R32", 16);
            case "QUARTER_FINAL" -> buildActualNextRound("ROUND_OF_16", "R16", 8);
            case "SEMI_FINAL" -> buildActualNextRound("QUARTER_FINAL", "QF", 4);
            case "FINAL" -> buildActualNextRound("SEMI_FINAL", "SF", 2);
            case "THIRD_PLACE" -> buildActualThirdPlaceMatch();
            default -> throw new RuntimeException("Unknown round: " + roundName);
        };
    }

    public List<KnockoutMatchDto> buildActualThirdPlaceMatch() {
        List<KnockoutMatchDto> semiFinalMatches = buildActualKnockoutRound("SEMI_FINAL");

        List<ActualKnockoutResult> semiResults =
                actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc("SEMI_FINAL");

        if (semiResults.size() != 2) {
            throw new RuntimeException("You must save both semi finals first");
        }

        List<String> losers = new ArrayList<>();

        for (int i = 0; i < semiFinalMatches.size(); i++) {
            KnockoutMatchDto match = semiFinalMatches.get(i);
            ActualKnockoutResult result = semiResults.get(i);

            String winner = result.getWinner().getName();

            String loser = match.getHomeTeam().equals(winner)
                    ? match.getAwayTeam()
                    : match.getHomeTeam();

            losers.add(loser);
        }

        return List.of(new KnockoutMatchDto(
                "Loser SF-1 vs Loser SF-2",
                losers.get(0),
                losers.get(1)
        ));
    }

    @Transactional
    public ActualKnockoutResult saveActualKnockoutResult(String roundName,
                                                         int matchNumber,
                                                         String slot,
                                                         String homeTeamName,
                                                         String awayTeamName,
                                                         int homeScore,
                                                         int awayScore,
                                                         Integer homePenaltyScore,
                                                         Integer awayPenaltyScore) {
        clearActualFutureRounds(roundName);

        Team homeTeam = teamRepository.findByName(homeTeamName)
                .orElseThrow(() -> new RuntimeException("Home team not found: " + homeTeamName));

        Team awayTeam = teamRepository.findByName(awayTeamName)
                .orElseThrow(() -> new RuntimeException("Away team not found: " + awayTeamName));

        Team winner;

        if (homeScore > awayScore) {
            winner = homeTeam;
            homePenaltyScore = null;
            awayPenaltyScore = null;
        } else if (awayScore > homeScore) {
            winner = awayTeam;
            homePenaltyScore = null;
            awayPenaltyScore = null;
        } else {
            if (homePenaltyScore == null || awayPenaltyScore == null) {
                throw new RuntimeException("Penalty score is required when knockout match is tied");
            }

            if (homePenaltyScore.equals(awayPenaltyScore)) {
                throw new RuntimeException("Penalty score cannot be tied");
            }

            winner = homePenaltyScore > awayPenaltyScore ? homeTeam : awayTeam;
        }

        ActualKnockoutResult result = actualKnockoutResultRepository
                .findByRoundNameAndMatchNumber(roundName, matchNumber)
                .orElse(ActualKnockoutResult.builder()
                        .roundName(roundName)
                        .matchNumber(matchNumber)
                        .build());

        result.setSlot(slot);
        result.setHomeTeam(homeTeam);
        result.setAwayTeam(awayTeam);
        result.setHomeScore(homeScore);
        result.setAwayScore(awayScore);
        result.setHomePenaltyScore(homePenaltyScore);
        result.setAwayPenaltyScore(awayPenaltyScore);
        result.setPlayed(true);
        result.setWinner(winner);

        ActualKnockoutResult saved = actualKnockoutResultRepository.saveAndFlush(result);

        rebuildActualQualifiedTeamsForRound(roundName);

        return saved;
    }

    private void rebuildActualQualifiedTeamsForRound(String roundName) {
        String nextStage = switch (roundName) {
            case "ROUND_OF_32" -> "ROUND_OF_16";
            case "ROUND_OF_16" -> "QUARTER_FINAL";
            case "QUARTER_FINAL" -> "SEMI_FINAL";
            case "SEMI_FINAL" -> "FINAL";
            case "FINAL" -> "CHAMPION";
            case "THIRD_PLACE" -> "THIRD_PLACE";
            default -> null;
        };

        if (nextStage == null) return;

        actualQualifiedTeamRepository.deleteByStage(nextStage);

        if ("FINAL".equals(roundName)) {
            actualQualifiedTeamRepository.deleteByStage("RUNNER_UP");
        }

        if ("THIRD_PLACE".equals(roundName)) {
            actualQualifiedTeamRepository.deleteByStage("FOURTH_PLACE");
        }

        actualQualifiedTeamRepository.flush();

        List<ActualKnockoutResult> results =
                actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc(roundName);

        for (ActualKnockoutResult result : results) {
            if (!result.isPlayed() || result.getWinner() == null) {
                continue;
            }

            actualQualifiedTeamRepository.save(
                    ActualQualifiedTeam.builder()
                            .team(result.getWinner())
                            .stage(nextStage)
                            .build()
            );

            if ("FINAL".equals(roundName)) {
                Team runnerUp = result.getHomeTeam().getName().equals(result.getWinner().getName())
                        ? result.getAwayTeam()
                        : result.getHomeTeam();

                actualQualifiedTeamRepository.save(
                        ActualQualifiedTeam.builder()
                                .team(runnerUp)
                                .stage("RUNNER_UP")
                                .build()
                );
            }

            if ("THIRD_PLACE".equals(roundName)) {
                Team fourthPlace = result.getHomeTeam().getName().equals(result.getWinner().getName())
                        ? result.getAwayTeam()
                        : result.getHomeTeam();

                actualQualifiedTeamRepository.save(
                        ActualQualifiedTeam.builder()
                                .team(fourthPlace)
                                .stage("FOURTH_PLACE")
                                .build()
                );
            }
        }

        actualQualifiedTeamRepository.flush();
    }

    private void clearActualFutureRounds(String roundName) {
        List<String> futureRounds = switch (roundName) {
            case "ROUND_OF_32" -> List.of("ROUND_OF_16", "QUARTER_FINAL", "SEMI_FINAL", "FINAL", "THIRD_PLACE");
            case "ROUND_OF_16" -> List.of("QUARTER_FINAL", "SEMI_FINAL", "FINAL", "THIRD_PLACE");
            case "QUARTER_FINAL" -> List.of("SEMI_FINAL", "FINAL", "THIRD_PLACE");
            case "SEMI_FINAL" -> List.of("FINAL", "THIRD_PLACE");
            case "FINAL", "THIRD_PLACE" -> List.of();
            default -> List.of();
        };

        List<String> futureStages = switch (roundName) {
            case "ROUND_OF_32" -> List.of("ROUND_OF_16", "QUARTER_FINAL", "SEMI_FINAL", "FINAL", "CHAMPION", "RUNNER_UP", "THIRD_PLACE", "FOURTH_PLACE");
            case "ROUND_OF_16" -> List.of("QUARTER_FINAL", "SEMI_FINAL", "FINAL", "CHAMPION", "RUNNER_UP", "THIRD_PLACE", "FOURTH_PLACE");
            case "QUARTER_FINAL" -> List.of("SEMI_FINAL", "FINAL", "CHAMPION", "RUNNER_UP", "THIRD_PLACE", "FOURTH_PLACE");
            case "SEMI_FINAL" -> List.of("FINAL", "CHAMPION", "RUNNER_UP", "THIRD_PLACE", "FOURTH_PLACE");
            case "FINAL" -> List.of("CHAMPION", "RUNNER_UP");
            case "THIRD_PLACE" -> List.of("THIRD_PLACE", "FOURTH_PLACE");
            default -> List.of();
        };

        for (String round : futureRounds) {
            actualKnockoutResultRepository.deleteByRoundName(round);
        }

        for (String stage : futureStages) {
            actualQualifiedTeamRepository.deleteByStage(stage);
        }
    }

    public List<ActualKnockoutResult> getActualKnockoutResults(String roundName) {
        return actualKnockoutResultRepository.findByRoundNameOrderByMatchNumberAsc(roundName);
    }

    @Transactional
    public ActualKnockoutResult resetActualKnockoutResult(String roundName, int matchNumber) {
        ActualKnockoutResult result = actualKnockoutResultRepository
                .findByRoundNameAndMatchNumber(roundName, matchNumber)
                .orElseThrow(() -> new RuntimeException("Actual knockout result not found"));

        result.setHomeScore(null);
        result.setAwayScore(null);
        result.setHomePenaltyScore(null);
        result.setAwayPenaltyScore(null);
        result.setPlayed(false);
        result.setWinner(null);

        ActualKnockoutResult saved = actualKnockoutResultRepository.saveAndFlush(result);

        clearActualFutureRounds(roundName);
        rebuildActualQualifiedTeamsForRound(roundName);

        return saved;
    }
}