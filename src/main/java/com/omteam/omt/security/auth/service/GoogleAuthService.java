package com.omteam.omt.security.auth.service;

import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.user.domain.SocialProvider;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserSocialAccount;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserRepository;
import com.omteam.omt.user.repository.UserSocialAccountRepository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserRepository userRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final WebClient webClient;

    @Value("${oauth.google.client-id}")
    private String clientId;
    @Value("${oauth.google.client-secret}")
    private String clientSecret;
    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;
    @Value("${oauth.google.token-uri}")
    private String tokenUri;
    @Value("${oauth.google.userinfo-uri}")
    private String userInfoUri;

    public LoginResponse login(String authorizationCode) {
        String decodedCode = decodeCode(authorizationCode);

        Map<String, Object> tokenResponse = requestToken(decodedCode);
        String accessToken = (String) tokenResponse.get("access_token");
        log.info("access Token: {}", accessToken);
        Map<String, Object> userInfo = requestUserInfo(accessToken);

        String providerUserId = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        log.info("providerUserId: {}", providerUserId);

        // 신규 / 기존 유저 처리
        User user = findOrCreateUser(providerUserId, email);
        boolean onboardingCompleted = user.isOnboardingCompleted();

        LoginResponse response = issueServerToken(user.getUserId());
        response.setOnboardingCompleted(onboardingCompleted);

        return response;
    }


    private String decodeCode(String code) {
        try {
            // 이미 디코딩된 상태일 수도 있으므로 % 문자가 포함된 경우에만 디코딩
            if (code != null && code.contains("%")) {
                return URLDecoder.decode(code, StandardCharsets.UTF_8);
            }
            return code;
        } catch (Exception e) {
            log.error("Code decoding failed", e);
            return code;
        }
    }

    private Map<String, Object> requestToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code); // 반드시 디코딩된 4/... 형태여야 함
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("grant_type", "authorization_code");

        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Google Token Error: " + body)))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
    }

    private Map<String, Object> requestUserInfo(String accessToken) {
        return webClient.get()
                .uri(userInfoUri)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
    }

    @Transactional
    public User findOrCreateUser(String providerUserId, String email) {
        Optional<UserSocialAccount> socialAccountOpt = userSocialAccountRepository.findByProviderUserId(providerUserId);
        Optional<User> userOpt = socialAccountOpt.map(UserSocialAccount::getUser);

        return userOpt.orElseGet(() -> createNewUser(providerUserId, email));
    }

    private User createNewUser(String providerUserId, String email) {
        // 2. 신규 유저 생성 (빌더 사용)
        User newUser = User.builder()
                .nickname(null) // 이후 온보딩에서 업데이트
                .email(email)
                .build();

        User savedUser = userRepository.save(newUser);

        // 3. 소셜 계정 정보 연동
        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .provider(SocialProvider.GOOGLE)
                .providerUserId(providerUserId)
                .user(savedUser)
                .build();
        userSocialAccountRepository.save(socialAccount);

        // 4. 캐릭터 기본 데이터 초기화
        UserCharacter character = UserCharacter.builder()
                .user(savedUser)
                .level(1)
                .totalActiveDays(0)
                .build();
        userCharacterRepository.save(character);

        return savedUser;
    }


    private LoginResponse issueServerToken(Long userId) {
        // JWT 발급 라이브러리(jjwt 등)를 사용한 구현체 호출
        //추가 필요
        return new LoginResponse("access-token-example", "refresh-token-example", 3600);
    }
}