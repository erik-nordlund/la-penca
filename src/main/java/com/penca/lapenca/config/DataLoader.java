package com.penca.lapenca.config;

import com.penca.lapenca.entity.Match;
import com.penca.lapenca.entity.Team;
import com.penca.lapenca.repository.MatchRepository;
import com.penca.lapenca.repository.TeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner loadData(TeamRepository teamRepository, MatchRepository matchRepository) {
        return args -> {

            if (teamRepository.count() == 0) {
                // Group A
                saveTeam(teamRepository, "Mexico", "A");
                saveTeam(teamRepository, "South Africa", "A");
                saveTeam(teamRepository, "South Korea", "A");
                saveTeam(teamRepository, "Czech Republic", "A");

                // Group B
                saveTeam(teamRepository, "Canada", "B");
                saveTeam(teamRepository, "Qatar", "B");
                saveTeam(teamRepository, "Switzerland", "B");
                saveTeam(teamRepository, "Bosnia", "B");

                // Group C
                saveTeam(teamRepository, "Brazil", "C");
                saveTeam(teamRepository, "Haiti", "C");
                saveTeam(teamRepository, "Scotland", "C");
                saveTeam(teamRepository, "Morocco", "C");

                // Group D
                saveTeam(teamRepository, "USA", "D");
                saveTeam(teamRepository, "Australia", "D");
                saveTeam(teamRepository, "Turkey", "D");
                saveTeam(teamRepository, "Paraguay", "D");

                // Group E
                saveTeam(teamRepository, "Germany", "E");
                saveTeam(teamRepository, "Ivory Coast", "E");
                saveTeam(teamRepository, "Ecuador", "E");
                saveTeam(teamRepository, "Curacao", "E");

                // Group F
                saveTeam(teamRepository, "Netherlands", "F");
                saveTeam(teamRepository, "Sweden", "F");
                saveTeam(teamRepository, "Tunisia", "F");
                saveTeam(teamRepository, "Japan", "F");

                // Group G
                saveTeam(teamRepository, "Belgium", "G");
                saveTeam(teamRepository, "Iran", "G");
                saveTeam(teamRepository, "New Zealand", "G");
                saveTeam(teamRepository, "Egypt", "G");

                // Group H
                saveTeam(teamRepository, "Spain", "H");
                saveTeam(teamRepository, "Saudi Arabia", "H");
                saveTeam(teamRepository, "Uruguay", "H");
                saveTeam(teamRepository, "Cape Verde", "H");

                // Group I
                saveTeam(teamRepository, "France", "I");
                saveTeam(teamRepository, "Iraq", "I");
                saveTeam(teamRepository, "Norway", "I");
                saveTeam(teamRepository, "Senegal", "I");

                // Group J
                saveTeam(teamRepository, "Argentina", "J");
                saveTeam(teamRepository, "Austria", "J");
                saveTeam(teamRepository, "Jordan", "J");
                saveTeam(teamRepository, "Algeria", "J");

                // Group K
                saveTeam(teamRepository, "Portugal", "K");
                saveTeam(teamRepository, "DR Congo", "K");
                saveTeam(teamRepository, "Uzbekistan", "K");
                saveTeam(teamRepository, "Colombia", "K");

                // Group L
                saveTeam(teamRepository, "England", "L");
                saveTeam(teamRepository, "Ghana", "L");
                saveTeam(teamRepository, "Panama", "L");
                saveTeam(teamRepository, "Croatia", "L");
            }

            if (matchRepository.count() == 0) {
                saveOfficialGroupMatches(teamRepository, matchRepository);
            }
        };
    }
    private void saveOfficialGroupMatches(TeamRepository teamRepository,
                                          MatchRepository matchRepository) {
        // Group A
        saveMatch(teamRepository, matchRepository, "Mexico", "South Africa", 2026, 6, 11, 21, 0);
        saveMatch(teamRepository, matchRepository, "South Korea", "Czech Republic", 2026, 6, 12, 4, 0);
        saveMatch(teamRepository, matchRepository, "Czech Republic", "South Africa", 2026, 6, 18, 18, 0);
        saveMatch(teamRepository, matchRepository, "Mexico", "South Korea", 2026, 6, 19, 3, 0);
        saveMatch(teamRepository, matchRepository, "South Africa", "South Korea", 2026, 6, 25, 3, 0);
        saveMatch(teamRepository, matchRepository, "Czech Republic", "Mexico", 2026, 6, 25, 3, 0);

        // Group B
        saveMatch(teamRepository, matchRepository, "Canada", "Bosnia", 2026, 6, 12, 21, 0);
        saveMatch(teamRepository, matchRepository, "Qatar", "Switzerland", 2026, 6, 13, 21, 0);
        saveMatch(teamRepository, matchRepository, "Switzerland", "Bosnia", 2026, 6, 18, 21, 0);
        saveMatch(teamRepository, matchRepository, "Canada", "Qatar", 2026, 6, 19, 0, 0);
        saveMatch(teamRepository, matchRepository, "Switzerland", "Canada", 2026, 6, 24, 21, 0);
        saveMatch(teamRepository, matchRepository, "Bosnia", "Qatar", 2026, 6, 24, 21, 0);

        // Group C
        saveMatch(teamRepository, matchRepository, "Brazil", "Morocco", 2026, 6, 14, 0, 0);
        saveMatch(teamRepository, matchRepository, "Haiti", "Scotland", 2026, 6, 14, 3, 0);
        saveMatch(teamRepository, matchRepository, "Scotland", "Morocco", 2026, 6, 20, 0, 0);
        saveMatch(teamRepository, matchRepository, "Brazil", "Haiti", 2026, 6, 20, 2, 30);
        saveMatch(teamRepository, matchRepository, "Morocco", "Haiti", 2026, 6, 25, 0, 0);
        saveMatch(teamRepository, matchRepository, "Scotland", "Brazil", 2026, 6, 25, 0, 0);

        // Group D
        saveMatch(teamRepository, matchRepository, "USA", "Paraguay", 2026, 6, 13, 3, 0);
        saveMatch(teamRepository, matchRepository, "Australia", "Turkey", 2026, 6, 14, 6, 0);
        saveMatch(teamRepository, matchRepository, "USA", "Australia", 2026, 6, 19, 21, 0);
        saveMatch(teamRepository, matchRepository, "Turkey", "Paraguay", 2026, 6, 20, 5, 0);
        saveMatch(teamRepository, matchRepository, "Turkey", "USA", 2026, 6, 26, 4, 0);
        saveMatch(teamRepository, matchRepository, "Paraguay", "Australia", 2026, 6, 26, 4, 0);

        // Group E
        saveMatch(teamRepository, matchRepository, "Germany", "Curacao", 2026, 6, 14, 19, 0);
        saveMatch(teamRepository, matchRepository, "Ivory Coast", "Ecuador", 2026, 6, 15, 1, 0);
        saveMatch(teamRepository, matchRepository, "Germany", "Ivory Coast", 2026, 6, 20, 22, 0);
        saveMatch(teamRepository, matchRepository, "Ecuador", "Curacao", 2026, 6, 21, 2, 0);
        saveMatch(teamRepository, matchRepository, "Curacao", "Ivory Coast", 2026, 6, 25, 22, 0);
        saveMatch(teamRepository, matchRepository, "Ecuador", "Germany", 2026, 6, 25, 22, 0);

        // Group F
        saveMatch(teamRepository, matchRepository, "Netherlands", "Japan", 2026, 6, 14, 22, 0);
        saveMatch(teamRepository, matchRepository, "Sweden", "Tunisia", 2026, 6, 15, 4, 0);
        saveMatch(teamRepository, matchRepository, "Netherlands", "Sweden", 2026, 6, 20, 19, 0);
        saveMatch(teamRepository, matchRepository, "Tunisia", "Japan", 2026, 6, 21, 6, 0);
        saveMatch(teamRepository, matchRepository, "Tunisia", "Netherlands", 2026, 6, 26, 1, 0);
        saveMatch(teamRepository, matchRepository, "Japan", "Sweden", 2026, 6, 26, 1, 0);

        // Group G
        saveMatch(teamRepository, matchRepository, "Belgium", "Egypt", 2026, 6, 15, 21, 0);
        saveMatch(teamRepository, matchRepository, "Iran", "New Zealand", 2026, 6, 16, 3, 0);
        saveMatch(teamRepository, matchRepository, "Belgium", "Iran", 2026, 6, 21, 21, 0);
        saveMatch(teamRepository, matchRepository, "New Zealand", "Egypt", 2026, 6, 22, 3, 0);
        saveMatch(teamRepository, matchRepository, "New Zealand", "Belgium", 2026, 6, 27, 5, 0);
        saveMatch(teamRepository, matchRepository, "Egypt", "Iran", 2026, 6, 27, 5, 0);

        // Group H
        saveMatch(teamRepository, matchRepository, "Spain", "Cape Verde", 2026, 6, 15, 18, 0);
        saveMatch(teamRepository, matchRepository, "Saudi Arabia", "Uruguay", 2026, 6, 16, 0, 0);
        saveMatch(teamRepository, matchRepository, "Spain", "Saudi Arabia", 2026, 6, 21, 18, 0);
        saveMatch(teamRepository, matchRepository, "Uruguay", "Cape Verde", 2026, 6, 22, 0, 0);
        saveMatch(teamRepository, matchRepository, "Cape Verde", "Saudi Arabia", 2026, 6, 27, 2, 0);
        saveMatch(teamRepository, matchRepository, "Uruguay", "Spain", 2026, 6, 27, 2, 0);

        // Group I
        saveMatch(teamRepository, matchRepository, "France", "Senegal", 2026, 6, 16, 21, 0);
        saveMatch(teamRepository, matchRepository, "Iraq", "Norway", 2026, 6, 17, 0, 0);
        saveMatch(teamRepository, matchRepository, "France", "Iraq", 2026, 6, 22, 23, 0);
        saveMatch(teamRepository, matchRepository, "Norway", "Senegal", 2026, 6, 23, 2, 0);
        saveMatch(teamRepository, matchRepository, "Norway", "France", 2026, 6, 26, 21, 0);
        saveMatch(teamRepository, matchRepository, "Senegal", "Iraq", 2026, 6, 26, 21, 0);

        // Group J
        saveMatch(teamRepository, matchRepository, "Argentina", "Algeria", 2026, 6, 17, 3, 0);
        saveMatch(teamRepository, matchRepository, "Austria", "Jordan", 2026, 6, 17, 6, 0);
        saveMatch(teamRepository, matchRepository, "Argentina", "Austria", 2026, 6, 22, 19, 0);
        saveMatch(teamRepository, matchRepository, "Jordan", "Algeria", 2026, 6, 23, 5, 0);
        saveMatch(teamRepository, matchRepository, "Algeria", "Austria", 2026, 6, 28, 4, 0);
        saveMatch(teamRepository, matchRepository, "Jordan", "Argentina", 2026, 6, 28, 4, 0);

        // Group K
        saveMatch(teamRepository, matchRepository, "Portugal", "DR Congo", 2026, 6, 17, 19, 0);
        saveMatch(teamRepository, matchRepository, "Uzbekistan", "Colombia", 2026, 6, 18, 4, 0);
        saveMatch(teamRepository, matchRepository, "Portugal", "Uzbekistan", 2026, 6, 23, 19, 0);
        saveMatch(teamRepository, matchRepository, "Colombia", "DR Congo", 2026, 6, 24, 4, 0);
        saveMatch(teamRepository, matchRepository, "Colombia", "Portugal", 2026, 6, 28, 1, 30);
        saveMatch(teamRepository, matchRepository, "DR Congo", "Uzbekistan", 2026, 6, 28, 1, 30);

        // Group L
        saveMatch(teamRepository, matchRepository, "England", "Croatia", 2026, 6, 17, 22, 0);
        saveMatch(teamRepository, matchRepository, "Ghana", "Panama", 2026, 6, 18, 1, 0);
        saveMatch(teamRepository, matchRepository, "England", "Ghana", 2026, 6, 23, 22, 0);
        saveMatch(teamRepository, matchRepository, "Panama", "Croatia", 2026, 6, 24, 1, 0);
        saveMatch(teamRepository, matchRepository, "Panama", "England", 2026, 6, 27, 23, 0);
        saveMatch(teamRepository, matchRepository, "Croatia", "Ghana", 2026, 6, 27, 23, 0);
    }

    private void saveMatch(TeamRepository teamRepository,
                           MatchRepository matchRepository,
                           String homeTeamName,
                           String awayTeamName,
                           int year,
                           int month,
                           int day,
                           int hour,
                           int minute) {
        Team homeTeam = teamRepository.findByName(homeTeamName)
                .orElseThrow(() -> new RuntimeException("Team not found: " + homeTeamName));

        Team awayTeam = teamRepository.findByName(awayTeamName)
                .orElseThrow(() -> new RuntimeException("Team not found: " + awayTeamName));

        matchRepository.save(
                Match.builder()
                        .homeTeam(homeTeam)
                        .awayTeam(awayTeam)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.of(year, month, day, hour, minute))
                        .played(false)
                        .build()
        );
    }

    private void saveTeam(TeamRepository teamRepository, String name, String groupName) {
        if (teamRepository.findByName(name).isEmpty()) {
            teamRepository.save(
                    Team.builder()
                            .name(name)
                            .groupName(groupName)
                            .qualified(false)
                            .build()
            );
        }
    }
}