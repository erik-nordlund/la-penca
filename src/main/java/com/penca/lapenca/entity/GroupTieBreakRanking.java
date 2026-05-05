package com.penca.lapenca.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTieBreakRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    private int positionIndex;

    @ManyToOne
    private AppUser user;

    @ManyToOne
    private Party party;

    @ManyToOne
    private Team team;
}