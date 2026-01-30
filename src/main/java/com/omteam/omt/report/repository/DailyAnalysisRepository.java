package com.omteam.omt.report.repository;

import com.omteam.omt.report.domain.DailyAnalysis;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyAnalysisRepository extends JpaRepository<DailyAnalysis, Long> {

    Optional<DailyAnalysis> findByUserUserIdAndTargetDate(Long userId, LocalDate targetDate);

    boolean existsByUserUserIdAndTargetDate(Long userId, LocalDate targetDate);
}
