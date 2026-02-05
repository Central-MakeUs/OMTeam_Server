package com.omteam.omt.security.auth.jwt;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.config.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-characters-long";
    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 300;
    private static final long REFRESH_TOKEN_EXPIRE_SECONDS = 1209600;
    private static final String ISSUER = "omt-server";
    private static final String AUDIENCE = "omt-client";
    private static final Long USER_ID = 42L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setAccessTokenExpireSeconds(ACCESS_TOKEN_EXPIRE_SECONDS);
        jwtProperties.setRefreshTokenExpireSeconds(REFRESH_TOKEN_EXPIRE_SECONDS);
        jwtProperties.setIssuer(ISSUER);
        jwtProperties.setAudience(AUDIENCE);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    /* ============================= */
    /* ===== Token 생성 테스트 ======= */
    /* ============================= */

    @Test
    @DisplayName("액세스 토큰 생성 성공 - 생성된 토큰이 유효하고 subject에 userId가 포함됨")
    void createAccessToken_success() {
        // given / when
        String accessToken = jwtTokenProvider.createAccessToken(USER_ID);

        // then
        assertThat(accessToken).isNotNull().isNotBlank();
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.getUserId(accessToken)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공 - 생성된 토큰이 유효하고 subject에 userId가 포함됨")
    void createRefreshToken_success() {
        // given / when
        String refreshToken = jwtTokenProvider.createRefreshToken(USER_ID);

        // then
        assertThat(refreshToken).isNotNull().isNotBlank();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.getUserId(refreshToken)).isEqualTo(USER_ID);
    }

    /* ============================= */
    /* ===== Token 검증 테스트 ===== */
    /* ============================= */

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_success() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID);

        // when
        boolean result = jwtTokenProvider.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    void validateToken_fail_invalidToken() {
        // given
        String invalidToken = "this.is.not-a-valid-jwt-token";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_fail_expiredToken() {
        // given - expireSeconds를 음수로 설정하여 과거 시각의 만료일을 생성
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(SECRET);
        expiredProps.setAccessTokenExpireSeconds(-1);
        expiredProps.setRefreshTokenExpireSeconds(REFRESH_TOKEN_EXPIRE_SECONDS);
        expiredProps.setIssuer(ISSUER);
        expiredProps.setAudience(AUDIENCE);

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        String expiredToken = expiredProvider.createAccessToken(USER_ID);

        // when
        boolean result = jwtTokenProvider.validateToken(expiredToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 issuer 토큰 검증 실패")
    void validateToken_fail_wrongIssuer() {
        // given
        JwtProperties wrongIssuerProps = new JwtProperties();
        wrongIssuerProps.setSecret(SECRET);
        wrongIssuerProps.setAccessTokenExpireSeconds(ACCESS_TOKEN_EXPIRE_SECONDS);
        wrongIssuerProps.setRefreshTokenExpireSeconds(REFRESH_TOKEN_EXPIRE_SECONDS);
        wrongIssuerProps.setIssuer("wrong-issuer");
        wrongIssuerProps.setAudience(AUDIENCE);

        JwtTokenProvider wrongIssuerProvider = new JwtTokenProvider(wrongIssuerProps);
        String wrongIssuerToken = wrongIssuerProvider.createAccessToken(USER_ID);

        // when
        boolean result = jwtTokenProvider.validateToken(wrongIssuerToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 audience 토큰 검증 실패")
    void validateToken_fail_wrongAudience() {
        // given
        JwtProperties wrongAudienceProps = new JwtProperties();
        wrongAudienceProps.setSecret(SECRET);
        wrongAudienceProps.setAccessTokenExpireSeconds(ACCESS_TOKEN_EXPIRE_SECONDS);
        wrongAudienceProps.setRefreshTokenExpireSeconds(REFRESH_TOKEN_EXPIRE_SECONDS);
        wrongAudienceProps.setIssuer(ISSUER);
        wrongAudienceProps.setAudience("wrong-audience");

        JwtTokenProvider wrongAudienceProvider = new JwtTokenProvider(wrongAudienceProps);
        String wrongAudienceToken = wrongAudienceProvider.createAccessToken(USER_ID);

        // when
        boolean result = jwtTokenProvider.validateToken(wrongAudienceToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 서명 토큰 검증 실패")
    void validateToken_fail_wrongSignature() {
        // given - 다른 secret key로 서명된 토큰 생성
        String differentSecret = "different-secret-key-that-is-at-least-32-characters-long";
        SecretKey wrongKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        String wrongSignatureToken = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(String.valueOf(USER_ID))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_SECONDS * 1000))
                .signWith(wrongKey)
                .compact();

        // when
        boolean result = jwtTokenProvider.validateToken(wrongSignatureToken);

        // then
        assertThat(result).isFalse();
    }

    /* ============================= */
    /* ===== userId 추출 테스트 ==== */
    /* ============================= */

    @Test
    @DisplayName("userId 추출 성공")
    void getUserId_success() {
        // given
        String token = jwtTokenProvider.createAccessToken(USER_ID);

        // when
        Long extractedUserId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(extractedUserId).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("잘못된 subject 형식 시 null 반환")
    void getUserId_fail_invalidSubject() {
        // given - subject에 숫자가 아닌 문자열을 직접 설정한 토큰 생성
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String tokenWithInvalidSubject = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject("not-a-number")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_SECONDS * 1000))
                .signWith(key)
                .compact();

        // when
        Long result = jwtTokenProvider.getUserId(tokenWithInvalidSubject);

        // then
        assertThat(result).isNull();
    }

    /* ===================================== */
    /* ===== 만료 시간 반환 테스트 ========== */
    /* ===================================== */

    @Test
    @DisplayName("액세스 토큰 만료 시간 반환 확인")
    void getAccessTokenExpireSeconds_returnsCorrectValue() {
        // when
        long result = jwtTokenProvider.getAccessTokenExpireSeconds();

        // then
        assertThat(result).isEqualTo(ACCESS_TOKEN_EXPIRE_SECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰 만료 시간 반환 확인")
    void getRefreshTokenExpireSeconds_returnsCorrectValue() {
        // when
        long result = jwtTokenProvider.getRefreshTokenExpireSeconds();

        // then
        assertThat(result).isEqualTo(REFRESH_TOKEN_EXPIRE_SECONDS);
    }
}
