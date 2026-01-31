package com.omteam.omt.report.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiWeeklyAnalysisResponse {

    // 1. 이번주 실패 원인 순위 (사용자 입력 텍스트를 AI가 카테고리화 후 집계)
    private List<FailureReasonRank> failureReasonRanking;

    // 2. 이번주 결과에 대한 피드백
    private String weeklyFeedback;

    // 3. 지난 한달 요일별 결과 기반 피드백 (제목 + 본문)
    private DayOfWeekFeedback dayOfWeekFeedback;

    // 하위 호환성 유지
    private String mainFailureReason;
    private String overallFeedback;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FailureReasonRank {
        private int rank;           // 순위 (1, 2, 3...)
        private String category;    // AI가 분류한 카테고리 (예: "시간 부족", "피로", "날씨")
        private int count;          // 해당 카테고리 발생 횟수
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DayOfWeekFeedback {
        private String title;       // 피드백 제목 (예: "화요일에 집중해보세요")
        private String content;     // 피드백 본문 (실패 원인 분석 또는 개선 방안)
    }
}
