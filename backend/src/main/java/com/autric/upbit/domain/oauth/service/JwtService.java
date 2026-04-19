package com.autric.upbit.domain.oauth.service;

import com.autric.upbit.global.config.properties.AppJwtProperties;
import com.autric.upbit.global.response.code.ErrorCode;
import com.autric.upbit.global.response.exception.BusinessException;
import com.autric.upbit.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;
    private final AppJwtProperties jwtProperties;

    public void save(Long memberId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + memberId;
        long expirationSeconds = jwtProperties.getRefreshExpired() / 1000;

        redisTemplate.opsForValue().set(key, refreshToken, expirationSeconds, TimeUnit.SECONDS);
    }

    public String get(Long memberId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberId);
    }

    public void delete(Long memberId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId);
    }

    public boolean isValid(Long memberId, String refreshToken) {
        String stored = get(memberId);

        return stored != null && stored.equals(refreshToken);
    }

    public String reissueAccessToken(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || !jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtProvider.getMemberId(refreshToken);

        if (!isValid(memberId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        return jwtProvider.createAccessToken(memberId);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
