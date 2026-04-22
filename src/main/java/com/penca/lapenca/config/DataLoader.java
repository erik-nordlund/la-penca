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
                Team mexico = teamRepository.save(Team.builder().name("Mexico").groupName("A").qualified(false).build());
                Team southAfrica = teamRepository.save(Team.builder().name("South Africa").groupName("A").qualified(false).build());
                Team southKorea = teamRepository.save(Team.builder().name("South Korea").groupName("A").qualified(false).build());
                Team czechRepublic = teamRepository.save(Team.builder().name("Czech Republic").groupName("A").qualified(false).build());

                matchRepository.save(Match.builder()
                        .homeTeam(mexico)
                        .awayTeam(southAfrica)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(1))
                        .played(false)
                        .build());

                matchRepository.save(Match.builder()
                        .homeTeam(southKorea)
                        .awayTeam(czechRepublic)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(1))
                        .played(false)
                        .build());

                matchRepository.save(Match.builder()
                        .homeTeam(czechRepublic)
                        .awayTeam(southAfrica)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(2))
                        .played(false)
                        .build());

                matchRepository.save(Match.builder()
                        .homeTeam(mexico)
                        .awayTeam(southKorea)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(2))
                        .played(false)
                        .build());

                matchRepository.save(Match.builder()
                        .homeTeam(czechRepublic)
                        .awayTeam(mexico)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(3))
                        .played(false)
                        .build());

                matchRepository.save(Match.builder()
                        .homeTeam(southAfrica)
                        .awayTeam(southKorea)
                        .stage("GROUP")
                        .matchDate(LocalDateTime.now().plusDays(3))
                        .played(false)
                        .build());
            }
        };
    }
}