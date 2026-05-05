package com.penca.lapenca.dto;

import java.time.LocalDateTime;

public class PartyDto {

    private String code;
    private String name;
    private LocalDateTime predictionDeadline;
    private int memberCount;

    public PartyDto(String code, String name, LocalDateTime predictionDeadline, int memberCount) {
        this.code = code;
        this.name = name;
        this.predictionDeadline = predictionDeadline;
        this.memberCount = memberCount;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getPredictionDeadline() {
        return predictionDeadline;
    }

    public int getMemberCount() {
        return memberCount;
    }
}