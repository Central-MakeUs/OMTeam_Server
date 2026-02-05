package com.omteam.omt.security.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.omteam.omt.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    FilterChain filterChain;

    JwtAuthenticationFilter jwtAuthenticationFilter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    final String validToken = "valid.jwt.token";
    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰으로 인증 성공")
    void doFilterInternal_validToken_success() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer " + validToken);
        given(jwtTokenProvider.validateToken(validToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(validToken)).willReturn(userId);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) authentication.getPrincipal()).userId()).isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없으면 인증 설정하지 않음")
    void doFilterInternal_noToken_noAuthentication() throws ServletException, IOException {
        // given - Authorization 헤더 없음

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사 없는 토큰은 무시")
    void doFilterInternal_noBearerPrefix_noAuthentication() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", validToken);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(validToken);
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 인증 설정하지 않음")
    void doFilterInternal_invalidToken_noAuthentication() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer " + validToken);
        given(jwtTokenProvider.validateToken(validToken)).willReturn(false);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("getUserId가 null 반환 시 인증 설정하지 않음")
    void doFilterInternal_nullUserId_noAuthentication() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer " + validToken);
        given(jwtTokenProvider.validateToken(validToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(validToken)).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("validateToken에서 예외 발생 시에도 필터 체인 진행")
    void doFilterInternal_validateTokenException_filterChainContinues() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer " + validToken);
        given(jwtTokenProvider.validateToken(validToken)).willThrow(new RuntimeException("Token validation error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("getUserId에서 예외 발생 시에도 필터 체인 진행")
    void doFilterInternal_getUserIdException_filterChainContinues() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer " + validToken);
        given(jwtTokenProvider.validateToken(validToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(validToken)).willThrow(new RuntimeException("getUserId error"));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("빈 Authorization 헤더는 무시")
    void doFilterInternal_emptyAuthorizationHeader_noAuthentication() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 뒤에 토큰 없으면 빈 토큰으로 처리")
    void doFilterInternal_bearerOnly_noAuthentication() throws ServletException, IOException {
        // given
        request.addHeader("Authorization", "Bearer ");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
