package com.omteam.omt.security.auth.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(Long userId, String refreshToken, long expireSeconds) {
        redisTemplate.opsForValue().set(createKey(userId), refreshToken, expireSeconds, TimeUnit.SECONDS);
    }

    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get(createKey(userId));
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(createKey(userId));
    }

    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    private String createKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
