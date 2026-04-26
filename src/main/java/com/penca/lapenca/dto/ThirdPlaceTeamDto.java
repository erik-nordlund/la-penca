package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ThirdPlaceTeamDto {
    private String groupName;
    private String teamName;
    private int points;
}