package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class QualifiedTeamsOverviewDto {
    private List<String> groupWinners;
    private List<String> groupRunnersUp;
    private List<String> qualifiedThirdPlaceTeams;
}