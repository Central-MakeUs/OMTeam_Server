package com.omteam.omt.report.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiWeeklyAnalysisResponse {

    private String mainFailureReason;
    private String overallFeedback;
}
