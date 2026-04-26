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
                int day = 1;

                // Group A
                saveGroupMatches(teamRepository, matchRepository, "Mexico", "South Africa", "South Korea", "Czech Republic", day++);
                // Group B
                saveGroupMatches(teamRepository, matchRepository, "Canada", "Qatar", "Switzerland", "Bosnia", day++);
                // Group C
                saveGroupMatches(teamRepository, matchRepository, "Brazil", "Haiti", "Scotland", "Morocco", day++);
                // Group D
                saveGroupMatches(teamRepository, matchRepository, "USA", "Australia", "Turkey", "Paraguay", day++);
                // Group E
                saveGroupMatches(teamRepository, matchRepository, "Germany", "Ivory Coast", "Ecuador", "Curacao", day++);
                // Group F
                saveGroupMatches(teamRepository, matchRepository, "Netherlands", "Sweden", "Tunisia", "Japan", day++);
                // Group G
                saveGroupMatches(teamRepository, matchRepository, "Belgium", "Iran", "New Zealand", "Egypt", day++);
                // Group H
                saveGroupMatches(teamRepository, matchRepository, "Spain", "Saudi Arabia", "Uruguay", "Cape Verde", day++);
                // Group I
                saveGroupMatches(teamRepository, matchRepository, "France", "Iraq", "Norway", "Senegal", day++);
                // Group J
                saveGroupMatches(teamRepository, matchRepository, "Argentina", "Austria", "Jordan", "Algeria", day++);
                // Group K
                saveGroupMatches(teamRepository, matchRepository, "Portugal", "DR Congo", "Uzbekistan", "Colombia", day++);
                // Group L
                saveGroupMatches(teamRepository, matchRepository, "England", "Ghana", "Panama", "Croatia", day++);
            }
        };
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

    private void saveGroupMatches(TeamRepository teamRepository,
                                  MatchRepository matchRepository,
                                  String team1,
                                  String team2,
                                  String team3,
                                  String team4,
                                  int startDay) {

        Team t1 = teamRepository.findByName(team1).orElseThrow();
        Team t2 = teamRepository.findByName(team2).orElseThrow();
        Team t3 = teamRepository.findByName(team3).orElseThrow();
        Team t4 = teamRepository.findByName(team4).orElseThrow();

        saveMatch(matchRepository, t1, t2, startDay, 10);
        saveMatch(matchRepository, t3, t4, startDay, 14);

        saveMatch(matchRepository, t3, t2, startDay + 1, 10);
        saveMatch(matchRepository, t1, t4, startDay + 1, 14);

        saveMatch(matchRepository, t3, t1, startDay + 2, 10);
        saveMatch(matchRepository, t4, t2, startDay + 2, 14);
    }

    private void saveMatch(MatchRepository matchRepository,
                           Team homeTeam,
                           Team awayTeam,
                           int dayOffset,
                           int hour) {
        matchRepository.save(
                Match.builder()
                        .homeTeam(homeTeam)
                        .awayTeam(awayTeam)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(dayOffset).withHour(hour).withMinute(0).withSecond(0).withNano(0))
                        .played(false)
                        .build()
        );
    }
}