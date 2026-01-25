package com.omteam.omt.report.repository;

import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyAiAnalysisRepository extends JpaRepository<WeeklyAiAnalysis, Long> {

    Optional<WeeklyAiAnalysis> findByUserUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    boolean existsByUserUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);
}
