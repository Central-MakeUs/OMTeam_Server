package com.omteam.omt.common.util;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 날짜 범위 관련 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateRangeUtils {

    public record DateRange(LocalDate startDate, LocalDate endDate) {}

    /**
     * 주어진 날짜가 속한 주의 월요일을 반환한다.
     * date가 null이면 오늘 날짜 기준으로 계산한다.
     */
    public static LocalDate getWeekStartDate(LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        return target.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 주 시작일(월요일)로부터 주 종료일(일요일)을 반환한다.
     */
    public static LocalDate getWeekEndDate(LocalDate weekStart) {
        return weekStart.plusDays(6);
    }

    /**
     * 오늘 기준으로 최근 N일 범위를 반환한다.
     */
    public static DateRange getRecentDaysRange(int days) {
        LocalDate today = LocalDate.now();
        return new DateRange(today.minusDays(days), today);
    }

    /**
     * 특정 연/월의 N번째 주 월요일을 반환한다.
     * 해당 월에 존재하지 않는 주차인 경우 BusinessException을 발생시킨다.
     */
    public static LocalDate getWeekStartOfMonth(int year, int month, int weekOfMonth) {
        if (weekOfMonth < 1) {
            throw new BusinessException(ErrorCode.INVALID_WEEK_OF_MONTH);
        }

        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate firstMonday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate targetMonday = firstMonday.plusDays((long) (weekOfMonth - 1) * 7);

        if (targetMonday.isAfter(lastDayOfMonth)) {
            throw new BusinessException(ErrorCode.INVALID_WEEK_OF_MONTH);
        }

        return targetMonday;
    }
}
