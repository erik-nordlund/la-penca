package com.penca.lapenca.dto;

public class PartyMemberDto {
    private String username;

    public PartyMemberDto(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}