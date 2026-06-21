package com.penca.lapenca.dto;

import com.penca.lapenca.entity.Match;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NextMatchPredictionsDto {

    private Match match;
    private List<MemberPrediction> members;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberPrediction {
        private String username;
        private String outcome; // "HOME_WIN", "DRAW", "AWAY_WIN", or null
    }
}