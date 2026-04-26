package com.penca.lapenca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnockoutPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roundName; // ROUND_OF_32, ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, FINAL

    private int matchNumber;

    private String slot;

    @ManyToOne
    private AppUser user;

    @ManyToOne
    private Party party;

    @ManyToOne
    private Team predictedWinner;
}