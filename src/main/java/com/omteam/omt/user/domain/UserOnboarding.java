package com.omteam.omt.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_onboarding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserOnboarding {

    @Id
    private Long userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String appGoalText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkTimeType workTimeType;

    @Column(nullable = false)
    private LocalTime availableStartTime;

    @Column(nullable = false)
    private LocalTime availableEndTime;

    @Column(nullable = false)
    private int minExerciseMinutes;

    @Column(nullable = false)
    private String preferredExerciseText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifestyleType lifestyleType;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void update(
            String appGoalText,
            WorkTimeType workTimeType,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            int minExerciseMinutes,
            String preferredExerciseText,
            LifestyleType lifestyleType
    ) {
        this.appGoalText = appGoalText;
        this.workTimeType = workTimeType;
        this.availableStartTime = availableStartTime;
        this.availableEndTime = availableEndTime;
        this.minExerciseMinutes = minExerciseMinutes;
        this.preferredExerciseText = preferredExerciseText;
        this.lifestyleType = lifestyleType;
    }
}
