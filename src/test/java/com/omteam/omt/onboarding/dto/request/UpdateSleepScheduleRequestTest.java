package com.omteam.omt.onboarding.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("[단위] UpdateSleepScheduleRequest 유효성 검증")
class UpdateSleepScheduleRequestTest {

    static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("wakeUpTime 검증")
    class WakeUpTimeValidation {

        @Test
        @DisplayName("null이면 유효하다")
        void valid_null() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(null);

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("분이 0이면 유효하다")
        void valid_minuteZero() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(7, 0));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("분이 30이면 유효하다")
        void valid_minuteThirty() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(7, 30));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("분이 15이면 유효하지 않다")
        void invalid_minuteFifteen() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(7, 15));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("기상 시간은 30분 단위여야 합니다 (예: 07:00, 07:30)");
        }

        @Test
        @DisplayName("분이 45이면 유효하지 않다")
        void invalid_minuteFortyFive() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(7, 45));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
        }
    }

    @Nested
    @DisplayName("bedTime 검증")
    class BedTimeValidation {

        @Test
        @DisplayName("null이면 유효하다")
        void valid_null() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setBedTime(null);

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("분이 0이면 유효하다")
        void valid_minuteZero() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setBedTime(LocalTime.of(23, 0));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("분이 30이면 유효하다")
        void valid_minuteThirty() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setBedTime(LocalTime.of(23, 30));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("분이 10이면 유효하지 않다")
        void invalid_minuteTen() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setBedTime(LocalTime.of(23, 10));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("취침 시간은 30분 단위여야 합니다 (예: 23:00, 23:30)");
        }
    }

    @Nested
    @DisplayName("기상/취침 시간 동시 설정")
    class BothTimesValidation {

        @Test
        @DisplayName("두 시간 모두 유효한 30분 단위이면 유효하다")
        void valid_bothValid() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(6, 30));
            request.setBedTime(LocalTime.of(22, 30));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("두 시간 모두 null이면 유효하다")
        void valid_bothNull() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("wakeUpTime이 유효하지 않으면 violations가 1개 발생한다")
        void invalid_wakeUpTimeOnly() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(7, 20));
            request.setBedTime(LocalTime.of(23, 0));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
        }

        @Test
        @DisplayName("두 시간 모두 유효하지 않으면 violations가 2개 발생한다")
        void invalid_both() {
            UpdateSleepScheduleRequest request = new UpdateSleepScheduleRequest();
            request.setWakeUpTime(LocalTime.of(7, 15));
            request.setBedTime(LocalTime.of(23, 15));

            Set<ConstraintViolation<UpdateSleepScheduleRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(2);
        }
    }
}
