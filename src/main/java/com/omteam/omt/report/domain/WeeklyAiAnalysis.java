package com.omteam.omt.report.domain;

import com.omteam.omt.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "weekly_ai_analysis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_weekly_ai_analysis_user_week",
                columnNames = {"user_id", "week_start_date"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WeeklyAiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(length = 200)
    private String mainFailureReason;

    @Column(length = 500)
    private String overallFeedback;

    @Column(columnDefinition = "TEXT")
    private String failureReasonRankingJson;

    @Column(length = 1000)
    private String weeklyFeedback;

    @Column(length = 100)
    private String dayOfWeekFeedbackTitle;

    @Column(length = 1000)
    private String dayOfWeekFeedbackContent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
