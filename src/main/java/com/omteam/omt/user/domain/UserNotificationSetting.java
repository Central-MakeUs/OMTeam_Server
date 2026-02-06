package com.omteam.omt.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserNotificationSetting {

    @Id
    private Long userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private boolean remindEnabled;

    @Column(nullable = false)
    private boolean checkinEnabled;

    @Column(nullable = false)
    private boolean reviewEnabled;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void update(boolean remindEnabled, boolean checkinEnabled, boolean reviewEnabled) {
        this.remindEnabled = remindEnabled;
        this.checkinEnabled = checkinEnabled;
        this.reviewEnabled = reviewEnabled;
    }

    public void updateNotification(NotificationType type, boolean enabled) {
        switch (type) {
            case REMIND -> this.remindEnabled = enabled;
            case CHECKIN -> this.checkinEnabled = enabled;
            case REVIEW -> this.reviewEnabled = enabled;
        }
    }
}
