package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupResultDto {
    private String groupName;
    private String firstPlace;
    private String secondPlace;
    private String thirdPlace;
}