package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class KnockoutBracketDto {

    private List<KnockoutMatchDto> roundOf32;
    private List<KnockoutMatchDto> roundOf16;
    private List<KnockoutMatchDto> quarterFinals;
    private List<KnockoutMatchDto> semiFinals;
    private List<KnockoutMatchDto> finalMatch;
    private String champion;
}