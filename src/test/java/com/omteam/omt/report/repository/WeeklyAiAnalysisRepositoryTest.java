package com.omteam.omt.report.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyAiAnalysisRepositoryTest {

    @Mock
    WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    User testUser;
    WeeklyAiAnalysis testAnalysis;
    final LocalDate weekStartDate = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        testAnalysis = WeeklyAiAnalysis.builder()
                .id(1L)
                .user(testUser)
                .weekStartDate(weekStartDate)
                .mainFailureReason("시간 부족")
                .overallFeedback("이번 주 피드백입니다.")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("엔티티 저장 성공")
    void save_success() {
        // given
        given(weeklyAiAnalysisRepository.save(any(WeeklyAiAnalysis.class)))
                .willReturn(testAnalysis);

        WeeklyAiAnalysis toSave = WeeklyAiAnalysis.builder()
                .user(testUser)
                .weekStartDate(weekStartDate)
                .mainFailureReason("시간 부족")
                .overallFeedback("이번 주 피드백입니다.")
                .build();

        // when
        WeeklyAiAnalysis saved = weeklyAiAnalysisRepository.save(toSave);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(saved.getWeekStartDate()).isEqualTo(weekStartDate);
        assertThat(saved.getMainFailureReason()).isEqualTo("시간 부족");
        assertThat(saved.getOverallFeedback()).isEqualTo("이번 주 피드백입니다.");
    }

    @Test
    @DisplayName("ID로 조회 성공")
    void findById_success() {
        // given
        given(weeklyAiAnalysisRepository.findById(1L))
                .willReturn(Optional.of(testAnalysis));

        // when
        Optional<WeeklyAiAnalysis> found = weeklyAiAnalysisRepository.findById(1L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("userId와 weekStartDate로 조회 성공")
    void findByUserUserIdAndWeekStartDate_success() {
        // given
        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(1L, weekStartDate))
                .willReturn(Optional.of(testAnalysis));

        // when
        Optional<WeeklyAiAnalysis> found = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(1L, weekStartDate);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getUserId()).isEqualTo(1L);
        assertThat(found.get().getWeekStartDate()).isEqualTo(weekStartDate);
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회 시 empty 반환")
    void findByUserUserIdAndWeekStartDate_notFoundByUserId() {
        // given
        Long nonExistentUserId = 99999L;
        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(nonExistentUserId, weekStartDate))
                .willReturn(Optional.empty());

        // when
        Optional<WeeklyAiAnalysis> found = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(nonExistentUserId, weekStartDate);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 weekStartDate로 조회 시 empty 반환")
    void findByUserUserIdAndWeekStartDate_notFoundByWeekStartDate() {
        // given
        LocalDate differentWeekStartDate = LocalDate.of(2024, 1, 22);
        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(1L, differentWeekStartDate))
                .willReturn(Optional.empty());

        // when
        Optional<WeeklyAiAnalysis> found = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(1L, differentWeekStartDate);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("mainFailureReason과 overallFeedback이 null인 경우에도 조회 성공")
    void findById_with_null_optional_fields() {
        // given
        WeeklyAiAnalysis analysisWithNulls = WeeklyAiAnalysis.builder()
                .id(2L)
                .user(testUser)
                .weekStartDate(weekStartDate)
                .mainFailureReason(null)
                .overallFeedback(null)
                .createdAt(LocalDateTime.now())
                .build();

        given(weeklyAiAnalysisRepository.findById(2L))
                .willReturn(Optional.of(analysisWithNulls));

        // when
        Optional<WeeklyAiAnalysis> found = weeklyAiAnalysisRepository.findById(2L);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getMainFailureReason()).isNull();
        assertThat(found.get().getOverallFeedback()).isNull();
    }
}
