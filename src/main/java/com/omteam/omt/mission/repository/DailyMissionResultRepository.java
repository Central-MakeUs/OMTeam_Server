package com.omteam.omt.mission.repository;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyMissionResultRepository extends JpaRepository<DailyMissionResult, Long> {

    Optional<DailyMissionResult> findByUserUserIdAndMissionDate(Long userId, LocalDate missionDate);

    boolean existsByUserUserIdAndMissionDate(Long userId, LocalDate missionDate);

    List<DailyMissionResult> findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<DailyMissionResult> findByUserUserIdOrderByMissionDateDesc(Long userId);

    @Query("SELECT dmr FROM DailyMissionResult dmr " +
            "WHERE dmr.user.userId = :userId " +
            "AND dmr.missionDate >= :startDate " +
            "ORDER BY dmr.missionDate DESC")
    List<DailyMissionResult> findRecentResults(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate);

    @Query("SELECT dmr.failureReason FROM DailyMissionResult dmr " +
            "WHERE dmr.user.userId = :userId " +
            "AND dmr.result = :result " +
            "AND dmr.missionDate >= :startDate " +
            "AND dmr.missionDate <= :endDate " +
            "AND dmr.failureReason IS NOT NULL")
    List<String> findFailureReasonsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("result") MissionResult result,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
