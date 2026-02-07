package com.omteam.omt.common.util;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.common.util.DateRangeUtils.DateRange;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DateRangeUtilsTest {

    @Nested
    @DisplayName("getWeekStartDate 테스트")
    class GetWeekStartDateTest {

        @Test
        @DisplayName("월요일이 주어지면 같은 월요일을 반환한다")
        void givenMonday_returnsSameMonday() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15); // Monday

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartDate(monday);

            // then
            assertThat(weekStart).isEqualTo(monday);
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("수요일이 주어지면 이전 월요일을 반환한다")
        void givenWednesday_returnsPreviousMonday() {
            // given
            LocalDate wednesday = LocalDate.of(2024, 1, 17); // Wednesday

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartDate(wednesday);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("일요일이 주어지면 이전 월요일을 반환한다")
        void givenSunday_returnsPreviousMonday() {
            // given
            LocalDate sunday = LocalDate.of(2024, 1, 21); // Sunday

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartDate(sunday);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("null이 주어지면 이번 주의 월요일을 반환한다")
        void givenNull_returnsCurrentWeekMonday() {
            // given & when
            LocalDate weekStart = DateRangeUtils.getWeekStartDate(null);

            // then
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("화요일이 주어지면 이전 월요일을 반환한다")
        void givenTuesday_returnsPreviousMonday() {
            // given
            LocalDate tuesday = LocalDate.of(2024, 1, 16); // Tuesday

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartDate(tuesday);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("토요일이 주어지면 이번 주 월요일을 반환한다")
        void givenSaturday_returnsThisWeekMonday() {
            // given
            LocalDate saturday = LocalDate.of(2024, 1, 20); // Saturday

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartDate(saturday);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }
    }

    @Nested
    @DisplayName("getWeekEndDate 테스트")
    class GetWeekEndDateTest {

        @Test
        @DisplayName("월요일 2024-01-15가 주어지면 일요일 2024-01-21을 반환한다")
        void givenMonday_returnsSunday() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);

            // when
            LocalDate weekEnd = DateRangeUtils.getWeekEndDate(monday);

            // then
            assertThat(weekEnd).isEqualTo(LocalDate.of(2024, 1, 21));
            assertThat(weekEnd.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        }

        @Test
        @DisplayName("임의의 날짜가 주어지면 6일 후를 반환한다")
        void givenArbitraryDate_returnsSixDaysLater() {
            // given
            LocalDate date = LocalDate.of(2024, 2, 5);

            // when
            LocalDate weekEnd = DateRangeUtils.getWeekEndDate(date);

            // then
            assertThat(weekEnd).isEqualTo(date.plusDays(6));
        }

        @Test
        @DisplayName("월 경계를 넘는 경우도 올바르게 처리한다")
        void givenDateCrossingMonth_returnsCorrectEndDate() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 29); // Monday

            // when
            LocalDate weekEnd = DateRangeUtils.getWeekEndDate(monday);

            // then
            assertThat(weekEnd).isEqualTo(LocalDate.of(2024, 2, 4));
            assertThat(weekEnd.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        }
    }

    @Nested
    @DisplayName("getRecentDaysRange 테스트")
    class GetRecentDaysRangeTest {

        @Test
        @DisplayName("30일 범위를 요청하면 30일 전부터 오늘까지의 범위를 반환한다")
        void given30Days_returns30DaysRange() {
            // given
            int days = 30;

            // when
            DateRange range = DateRangeUtils.getRecentDaysRange(days);

            // then
            LocalDate today = LocalDate.now();
            assertThat(range.startDate()).isEqualTo(today.minusDays(30));
            assertThat(range.endDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("0일 범위를 요청하면 시작일과 종료일이 모두 오늘이다")
        void given0Days_returnsTodayRange() {
            // given
            int days = 0;

            // when
            DateRange range = DateRangeUtils.getRecentDaysRange(days);

            // then
            LocalDate today = LocalDate.now();
            assertThat(range.startDate()).isEqualTo(today);
            assertThat(range.endDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("7일 범위를 요청하면 7일 전부터 오늘까지의 범위를 반환한다")
        void given7Days_returns7DaysRange() {
            // given
            int days = 7;

            // when
            DateRange range = DateRangeUtils.getRecentDaysRange(days);

            // then
            LocalDate today = LocalDate.now();
            assertThat(range.startDate()).isEqualTo(today.minusDays(7));
            assertThat(range.endDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("1일 범위를 요청하면 어제부터 오늘까지의 범위를 반환한다")
        void given1Day_returnsYesterdayToTodayRange() {
            // given
            int days = 1;

            // when
            DateRange range = DateRangeUtils.getRecentDaysRange(days);

            // then
            LocalDate today = LocalDate.now();
            assertThat(range.startDate()).isEqualTo(today.minusDays(1));
            assertThat(range.endDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("90일 범위를 요청하면 90일 전부터 오늘까지의 범위를 반환한다")
        void given90Days_returns90DaysRange() {
            // given
            int days = 90;

            // when
            DateRange range = DateRangeUtils.getRecentDaysRange(days);

            // then
            LocalDate today = LocalDate.now();
            assertThat(range.startDate()).isEqualTo(today.minusDays(90));
            assertThat(range.endDate()).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("getWeekStartOfMonth 테스트")
    class GetWeekStartOfMonthTest {

        @Test
        @DisplayName("2024년 1월 1주차 월요일은 2024-01-01이다 (1월 1일이 월요일)")
        void given2024Jan_week1_returnsFirstMonday() {
            // given
            int year = 2024;
            int month = 1;
            int weekOfMonth = 1;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("2024년 2월 1주차 월요일은 2024-02-05이다")
        void given2024Feb_week1_returnsFirstMonday() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = 1;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 2, 5));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("2024년 2월 2주차 월요일은 2024-02-12이다")
        void given2024Feb_week2_returnsSecondMonday() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = 2;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 2, 12));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("2024년 2월 4주차 월요일은 2024-02-26이다")
        void given2024Feb_week4_returnsFourthMonday() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = 4;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 2, 26));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("월이 월요일로 시작하는 경우 1주차 월요일은 해당 월 1일이다")
        void givenMonthStartingOnMonday_week1ReturnsFirstDay() {
            // given
            // 2024년 1월 1일은 월요일
            int year = 2024;
            int month = 1;
            int weekOfMonth = 1;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("월이 일요일로 시작하는 경우 1주차 월요일은 해당 월 2일이다")
        void givenMonthStartingOnSunday_week1ReturnsSecondDay() {
            // given
            // 2024년 9월 1일은 일요일
            int year = 2024;
            int month = 9;
            int weekOfMonth = 1;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 9, 2));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("weekOfMonth가 0이면 INVALID_WEEK_OF_MONTH 예외를 발생시킨다")
        void givenWeekOfMonth0_throwsBusinessException() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = 0;

            // when & then
            assertThatThrownBy(() -> DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }

        @Test
        @DisplayName("weekOfMonth가 음수이면 INVALID_WEEK_OF_MONTH 예외를 발생시킨다")
        void givenNegativeWeekOfMonth_throwsBusinessException() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = -1;

            // when & then
            assertThatThrownBy(() -> DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }

        @Test
        @DisplayName("2024년 2월 5주차 요청 시 INVALID_WEEK_OF_MONTH 예외를 발생시킨다 (2월은 4주차까지만 존재)")
        void given2024Feb_week5_throwsBusinessException() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = 5;

            // when & then
            assertThatThrownBy(() -> DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }

        @Test
        @DisplayName("weekOfMonth가 15이면 INVALID_WEEK_OF_MONTH 예외를 발생시킨다")
        void givenWeekOfMonth15_throwsBusinessException() {
            // given
            int year = 2024;
            int month = 2;
            int weekOfMonth = 15;

            // when & then
            assertThatThrownBy(() -> DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }

        @Test
        @DisplayName("2024년 1월 3주차 월요일은 2024-01-15이다")
        void given2024Jan_week3_returnsThirdMonday() {
            // given
            int year = 2024;
            int month = 1;
            int weekOfMonth = 3;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("2024년 12월 5주차 월요일은 2024-12-30이다")
        void given2024Dec_week5_returnsFifthMonday() {
            // given
            // 2024년 12월 1일은 일요일, 첫 번째 월요일은 12/2
            int year = 2024;
            int month = 12;
            int weekOfMonth = 5;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 12, 30));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("2024년 12월 6주차 요청 시 INVALID_WEEK_OF_MONTH 예외를 발생시킨다")
        void given2024Dec_week6_throwsBusinessException() {
            // given
            int year = 2024;
            int month = 12;
            int weekOfMonth = 6;

            // when & then
            assertThatThrownBy(() -> DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }

        @Test
        @DisplayName("월이 화요일로 시작하는 경우도 올바르게 처리한다")
        void givenMonthStartingOnTuesday_returnsCorrectFirstMonday() {
            // given
            // 2024년 10월 1일은 화요일, 첫 번째 월요일은 10/7
            int year = 2024;
            int month = 10;
            int weekOfMonth = 1;

            // when
            LocalDate weekStart = DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);

            // then
            assertThat(weekStart).isEqualTo(LocalDate.of(2024, 10, 7));
            assertThat(weekStart.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }
    }
}
