package com.omteam.omt.user.domain;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_onboarding")
@Getter
@NoArgsConstructor
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

    private String appGoalText;

//    @Enumerated(EnumType.STRING)
//    private WorkTimeType workTimeType;

    private LocalTime availableStartTime;

    private LocalTime availableEndTime;

    private int minExerciseMinutes;

    private String preferredExerciseText;

//    @Enumerated(EnumType.STRING)
//    private LifestyleType lifestyleType;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
