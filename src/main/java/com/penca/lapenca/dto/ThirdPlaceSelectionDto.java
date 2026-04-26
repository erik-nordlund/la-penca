package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ThirdPlaceSelectionDto {
    private List<ThirdPlaceTeamDto> qualifiedAutomatically;
    private List<ThirdPlaceTeamDto> tiedTeams;
    private int remainingSlots;
}