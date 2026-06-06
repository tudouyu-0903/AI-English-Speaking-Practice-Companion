package com.cxy.aienglishspeakingpracticecompanion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummaryResponse {
    private String summary;
    private Integer totalScore;
    private Integer fluencyScore;
    private Integer pronunciationScore;
    private Integer grammarScore;
    private List<String> corrections;
    private List<String> suggestions;
}