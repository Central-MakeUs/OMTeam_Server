package com.omteam.omt.report.service;

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
import java.time.temporal.TemporalAdjusters;
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
    public MonthlyPatternResponse getMonthlyPattern(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(MONTHLY_PATTERN_DAYS);

        List<DailyMissionResult> results = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(userId, monthAgo, today);

        boolean hasMissionData = !results.isEmpty();

        Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek = results.stream()
                .collect(Collectors.groupingBy(r -> r.getMissionDate().getDayOfWeek()));

        List<MonthlyPatternResponse.DayOfWeekStatistics> dayOfWeekStats =
                buildDayOfWeekStatistics(resultsByDayOfWeek);
        MonthlyPatternResponse.AiFeedback aiFeedback = getAiFeedback(userId, hasMissionData);

        ReportDataStatus dataStatus = ReportDataStatus.of(aiFeedback.isDefault(), hasMissionData);

        return MonthlyPatternResponse.builder()
                .dataStatus(dataStatus)
                .startDate(monthAgo)
                .endDate(today)
                .dayOfWeekStats(dayOfWeekStats)
                .aiFeedback(aiFeedback)
                .build();
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

    private MonthlyPatternResponse.AiFeedback getAiFeedback(Long userId, boolean hasMissionData) {
        LocalDate currentWeekMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Optional<WeeklyAiAnalysis> analysisOpt = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, currentWeekMonday);

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
