package com.omteam.omt.report.constant;

public final class DefaultReportMessages {

    private DefaultReportMessages() {
    }

    // 주간 피드백
    public static final String WEEKLY_PENDING = "매주 월요일에 AI가 한 주를 분석해드려요";
    public static final String WEEKLY_NO_DATA = "첫 미션을 완료하면 주간 분석이 시작돼요";

    // 월간 요일별 피드백
    public static final String MONTHLY_DAY_PENDING_TITLE = "아직 분석 준비 중이에요";
    public static final String MONTHLY_DAY_PENDING_CONTENT = "매주 월요일에 AI가 요일별 패턴을 분석해드려요";
    public static final String MONTHLY_DAY_NO_DATA_TITLE = "데이터가 아직 없어요";
    public static final String MONTHLY_DAY_NO_DATA_CONTENT = "첫 미션을 완료하면 요일별 패턴 분석이 시작돼요";

    // 데일리 피드백
    public static final String DAILY_NO_DATA = "오늘의 피드백이 아직 생성되지 않았어요";
}
