package com.omteam.omt.report.repository;

import com.omteam.omt.report.domain.AnalysisRetryQueue;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisRetryQueueRepository extends JpaRepository<AnalysisRetryQueue, Long> {

    List<AnalysisRetryQueue> findByStatus(String status);

    Optional<AnalysisRetryQueue> findByUserIdAndAnalysisTypeAndTargetDate(
            Long userId, String analysisType, LocalDate targetDate);
}
