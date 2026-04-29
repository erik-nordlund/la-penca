package com.penca.lapenca.dto;

public class LeaderboardRowDto {

    private String username;
    private int score;

    public LeaderboardRowDto(String username, int score) {
        this.username = username;
        this.score = score;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }
}