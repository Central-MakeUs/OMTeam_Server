package com.omteam.omt.mission.repository;

import com.omteam.omt.mission.domain.DailyRecommendedMission;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyRecommendedMissionRepository extends JpaRepository<DailyRecommendedMission, Long> {

    List<DailyRecommendedMission> findByUserUserIdAndMissionDateAndStatusNot(
            Long userId, LocalDate missionDate, RecommendedMissionStatus status);

    List<DailyRecommendedMission> findByUserUserIdAndMissionDateAndStatus(
            Long userId, LocalDate missionDate, RecommendedMissionStatus status);

    Optional<DailyRecommendedMission> findByUserUserIdAndMissionDateAndStatusIn(
            Long userId, LocalDate missionDate, List<RecommendedMissionStatus> statuses);

    @Query("SELECT drm FROM DailyRecommendedMission drm " +
            "WHERE drm.user.userId = :userId " +
            "AND drm.missionDate = :missionDate " +
            "AND drm.status IN :statuses")
    List<DailyRecommendedMission> findActiveRecommendations(
            @Param("userId") Long userId,
            @Param("missionDate") LocalDate missionDate,
            @Param("statuses") List<RecommendedMissionStatus> statuses);

    Optional<DailyRecommendedMission> findByIdAndUserUserId(Long id, Long userId);

    boolean existsByUserUserIdAndMissionDateAndStatusIn(
            Long userId, LocalDate missionDate, List<RecommendedMissionStatus> statuses);
}
