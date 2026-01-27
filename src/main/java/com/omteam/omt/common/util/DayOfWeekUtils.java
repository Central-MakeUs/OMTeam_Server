package com.omteam.omt.common.util;

import java.time.DayOfWeek;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 요일 관련 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DayOfWeekUtils {

    /**
     * DayOfWeek를 한글 요일명으로 변환
     */
    public static String toKorean(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }
}
