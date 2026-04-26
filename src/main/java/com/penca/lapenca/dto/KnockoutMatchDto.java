package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KnockoutMatchDto {
    private String slot;
    private String homeTeam;
    private String awayTeam;
}