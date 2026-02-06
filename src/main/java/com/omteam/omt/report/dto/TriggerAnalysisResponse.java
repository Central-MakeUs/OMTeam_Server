package com.omteam.omt.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TriggerAnalysisResponse {

    private int processedCount;
    private int successCount;
    private int failedCount;
    private String message;

    public static TriggerAnalysisResponse of(int processedCount, int successCount, String message) {
        return TriggerAnalysisResponse.builder()
                .processedCount(processedCount)
                .successCount(successCount)
                .failedCount(processedCount - successCount)
                .message(message)
                .build();
    }

    public static TriggerAnalysisResponse singleSuccess(String message) {
        return TriggerAnalysisResponse.builder()
                .processedCount(1)
                .successCount(1)
                .failedCount(0)
                .message(message)
                .build();
    }
}
