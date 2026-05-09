package com.penca.lapenca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualKnockoutResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roundName;

    private int matchNumber;

    private String slot;

    @ManyToOne
    private Team homeTeam;

    @ManyToOne
    private Team awayTeam;

    private Integer homeScore;

    private Integer awayScore;

    private Integer homePenaltyScore;

    private Integer awayPenaltyScore;

    private boolean played;

    @ManyToOne
    private Team winner;
}