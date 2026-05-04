package com.penca.lapenca.dto;

public class ScoreBreakdownDto {

    private String username;
    private int groupPoints;
    private int roundOf32Points;
    private int roundOf16Points;
    private int quarterFinalPoints;
    private int semiFinalPoints;
    private int finalPoints;
    private int placementPoints;
    private int totalPoints;

    public ScoreBreakdownDto(String username,
                             int groupPoints,
                             int roundOf32Points,
                             int roundOf16Points,
                             int quarterFinalPoints,
                             int semiFinalPoints,
                             int finalPoints,
                             int placementPoints) {
        this.username = username;
        this.groupPoints = groupPoints;
        this.roundOf32Points = roundOf32Points;
        this.roundOf16Points = roundOf16Points;
        this.quarterFinalPoints = quarterFinalPoints;
        this.semiFinalPoints = semiFinalPoints;
        this.finalPoints = finalPoints;
        this.placementPoints = placementPoints;
        this.totalPoints = groupPoints
                + roundOf32Points
                + roundOf16Points
                + quarterFinalPoints
                + semiFinalPoints
                + finalPoints
                + placementPoints;
    }

    public String getUsername() { return username; }
    public int getGroupPoints() { return groupPoints; }
    public int getRoundOf32Points() { return roundOf32Points; }
    public int getRoundOf16Points() { return roundOf16Points; }
    public int getQuarterFinalPoints() { return quarterFinalPoints; }
    public int getSemiFinalPoints() { return semiFinalPoints; }
    public int getFinalPoints() { return finalPoints; }
    public int getPlacementPoints() { return placementPoints; }
    public int getTotalPoints() { return totalPoints; }
}