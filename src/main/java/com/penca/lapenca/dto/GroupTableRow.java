package com.penca.lapenca.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GroupTableRow {
    private String teamName;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int points;
}