package com.omteam.omt.config.properties;

import static org.assertj.core.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("모든 필드가 유효할 때 검증 통과")
    void validProperties_success() {
        // given
        JwtProperties properties = createValidProperties();

        // when
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(properties);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("secret이 32자 미만이면 검증 실패")
    void secret_tooShort_fail() {
        // given
        JwtProperties properties = createValidProperties();
        properties.setSecret("short-secret");  // 12자 (32자 미만)

        // when
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(properties);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
                "secret".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("32자")
        );
    }

    @Test
    @DisplayName("secret이 빈 문자열이면 검증 실패")
    void secret_blank_fail() {
        // given
        JwtProperties properties = createValidProperties();
        properties.setSecret("");

        // when
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(properties);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).allMatch(v -> "secret".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("secret이 null이면 검증 실패")
    void secret_null_fail() {
        // given
        JwtProperties properties = createValidProperties();
        properties.setSecret(null);

        // when
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(properties);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                "secret".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("필수")
        );
    }

    @Test
    @DisplayName("accessTokenExpireSeconds가 음수이면 검증 실패")
    void accessTokenExpireSeconds_negative_fail() {
        // given
        JwtProperties properties = createValidProperties();
        properties.setAccessTokenExpireSeconds(-1);

        // when
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(properties);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
                "accessTokenExpireSeconds".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("양수")
        );
    }

    @Test
    @DisplayName("accessTokenExpireSeconds가 0이면 검증 실패")
    void accessTokenExpireSeconds_zero_fail() {
        // given
        JwtProperties properties = createValidProperties();
        properties.setAccessTokenExpireSeconds(0);

        // when
        Set<ConstraintViolation<JwtProperties>> violations = validator.validate(properties);

        // then
        assertThat(violations).hasSize(1);
        assertThat(violations).anyMatch(v ->
                "accessTokenExpireSeconds".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("양수")
        );
    }

    @Test
    @DisplayName("기본값이 올바르게 설정된다")
    void defaultValues_correct() {
        // given
        JwtProperties properties = new JwtProperties();

        // when & then
        assertThat(properties.getAccessTokenExpireSeconds()).isEqualTo(300);
        assertThat(properties.getRefreshTokenExpireSeconds()).isEqualTo(1209600);
        assertThat(properties.getIssuer()).isEqualTo("omt-server");
        assertThat(properties.getAudience()).isEqualTo("omt-client");
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private JwtProperties createValidProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("this-is-a-valid-secret-key-for-jwt-test");  // 39자
        properties.setAccessTokenExpireSeconds(300);
        properties.setRefreshTokenExpireSeconds(1209600);
        return properties;
    }
}
