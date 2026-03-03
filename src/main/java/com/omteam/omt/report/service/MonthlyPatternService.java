package com.omteam.omt.report.service;

import com.omteam.omt.common.util.DateRangeUtils;
import com.omteam.omt.common.util.DayOfWeekUtils;
import com.omteam.omt.common.util.MissionResultStats;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.constant.DefaultReportMessages;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import com.omteam.omt.report.dto.ReportDataStatus;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyPatternService {

    private static final int MONTHLY_PATTERN_DAYS = 30;

    private final DailyMissionResultRepository missionResultRepository;
    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    /**
     * 월간 요일별 패턴 분석
     */
    public MonthlyPatternResponse getMonthlyPattern(Long userId, Integer year, Integer month, Integer weekOfMonth) {
        LocalDate weekStartDate = resolveWeekStartDate(year, month, weekOfMonth);
        LocalDate startDate = weekStartDate.minusDays(MONTHLY_PATTERN_DAYS);

        List<DailyMissionResult> results = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(userId, startDate, weekStartDate);

        boolean hasMissionData = !results.isEmpty();

        Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek = results.stream()
                .collect(Collectors.groupingBy(r -> r.getMissionDate().getDayOfWeek()));

        List<MonthlyPatternResponse.DayOfWeekStatistics> dayOfWeekStats =
                buildDayOfWeekStatistics(resultsByDayOfWeek);
        MonthlyPatternResponse.AiFeedback aiFeedback = getAiFeedback(userId, hasMissionData, weekStartDate);

        ReportDataStatus dataStatus = ReportDataStatus.of(aiFeedback.isDefault(), hasMissionData);

        return MonthlyPatternResponse.builder()
                .dataStatus(dataStatus)
                .startDate(startDate)
                .endDate(weekStartDate)
                .dayOfWeekStats(dayOfWeekStats)
                .aiFeedback(aiFeedback)
                .build();
    }

    private LocalDate resolveWeekStartDate(Integer year, Integer month, Integer weekOfMonth) {
        if (year == null || month == null || weekOfMonth == null) {
            return DateRangeUtils.getWeekStartDate(LocalDate.now());
        }
        return DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);
    }

    private List<MonthlyPatternResponse.DayOfWeekStatistics> buildDayOfWeekStatistics(
            Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek) {

        return Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    List<DailyMissionResult> dowResults = resultsByDayOfWeek.getOrDefault(dow, List.of());
                    MissionResultStats missionStats = MissionResultStats.from(dowResults);
                    return MonthlyPatternResponse.DayOfWeekStatistics.builder()
                            .dayOfWeek(dow)
                            .dayName(DayOfWeekUtils.toKorean(dow))
                            .totalCount(missionStats.totalCount())
                            .successCount(missionStats.successCount())
                            .failureCount(missionStats.failureCount())
                            .successRate(missionStats.successRate())
                            .build();
                })
                .toList();
    }

    private MonthlyPatternResponse.AiFeedback getAiFeedback(Long userId, boolean hasMissionData, LocalDate weekStartDate) {
        Optional<WeeklyAiAnalysis> analysisOpt = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, weekStartDate);

        if (analysisOpt.isEmpty()) {
            String defaultTitle = hasMissionData
                    ? DefaultReportMessages.MONTHLY_DAY_PENDING_TITLE
                    : DefaultReportMessages.MONTHLY_DAY_NO_DATA_TITLE;
            String defaultContent = hasMissionData
                    ? DefaultReportMessages.MONTHLY_DAY_PENDING_CONTENT
                    : DefaultReportMessages.MONTHLY_DAY_NO_DATA_CONTENT;
            return MonthlyPatternResponse.AiFeedback.builder()
                    .dayOfWeekFeedbackTitle(defaultTitle)
                    .dayOfWeekFeedbackContent(defaultContent)
                    .isDefault(true)
                    .build();
        }

        WeeklyAiAnalysis analysis = analysisOpt.get();
        return MonthlyPatternResponse.AiFeedback.builder()
                .dayOfWeekFeedbackTitle(analysis.getDayOfWeekFeedbackTitle())
                .dayOfWeekFeedbackContent(analysis.getDayOfWeekFeedbackContent())
                .isDefault(false)
                .build();
    }
}
