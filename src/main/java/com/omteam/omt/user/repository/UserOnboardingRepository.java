package com.omteam.omt.user.repository;

import com.omteam.omt.user.domain.UserOnboarding;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    Optional<UserOnboarding> findByUserId(Long userId);

    // 리마인드: availableStartTime == time
    @Query("SELECT uo FROM UserOnboarding uo JOIN FETCH uo.user u WHERE uo.availableStartTime = :time AND u.onboardingCompleted = true AND u.deletedAt IS NULL")
    List<UserOnboarding> findByAvailableStartTimeForNotification(@Param("time") LocalTime time);

    // 체크인: wakeUpTime == time, null이면 defaultTime과 비교
    @Query("SELECT uo FROM UserOnboarding uo JOIN FETCH uo.user u WHERE (uo.wakeUpTime = :time OR (uo.wakeUpTime IS NULL AND :time = :defaultTime)) AND u.onboardingCompleted = true AND u.deletedAt IS NULL")
    List<UserOnboarding> findByEffectiveWakeUpTime(@Param("time") LocalTime time, @Param("defaultTime") LocalTime defaultTime);

    // 회고: bedTime == targetBedTime (now + 1h), null이면 defaultReviewTime과 비교
    @Query("SELECT uo FROM UserOnboarding uo JOIN FETCH uo.user u WHERE (uo.bedTime = :targetBedTime OR (uo.bedTime IS NULL AND :now = :defaultReviewTime)) AND u.onboardingCompleted = true AND u.deletedAt IS NULL")
    List<UserOnboarding> findByBedTimeForReview(@Param("targetBedTime") LocalTime targetBedTime, @Param("now") LocalTime now, @Param("defaultReviewTime") LocalTime defaultReviewTime);
}
