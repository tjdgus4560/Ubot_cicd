package com.autric.upbit.global.security.jwt;

import com.autric.upbit.global.config.properties.AppJwtProperties;
import com.autric.upbit.global.response.code.ErrorCode;
import com.autric.upbit.global.response.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰의 생성, 검증, 파싱 등을 담당하는 유틸리티 클래스
 * AccessToken / RefreshToken 발급
 * 토큰 유효성 검증 및 사용자 식별자 추출
 */
@Component
@Slf4j
public class JwtProvider {

    private final SecretKey secret;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    /**
     * 설정 값을 주입받아 초기화하는 생성자
     *
     * @param secret JWT 서명에 사용할 비밀 키
     * @param accessTokenValidity AccessToken 유효 시간
     * @param refreshTokenValidity RefreshToken 유효 시간
     */
    public JwtProvider(AppJwtProperties jwtProperties) {
        this.secret = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = jwtProperties.getAccessExpired();
        this.refreshTokenValidity = jwtProperties.getRefreshExpired();
    }

    /**
     * 액세스 토큰을 발급하는 메서드
     *
     * @param memberId 토큰에 담길 사용자 ID
     * @return 생성된 액세스 토큰(JWT)
     */
    public String createAccessToken(Long memberId) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(secret)
                .compact();
    }

    /**
     * 리프레시 토큰을 발급하는 메서드
     *
     * @param memberId 토큰에 담길 사용자 ID
     * @return 생성된 리프레시 토큰(JWT)
     */
    public String createRefreshToken(Long memberId) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(secret)
                .compact();
    }


    /**
     * 토큰에서 사용자 ID를 추출하는 메서드
     *
     * @param token 사용자 정보를 담고 있는 JWT
     * @return 추출된 사용자 ID
     */
    public Long getMemberId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.parseLong(claims.getSubject());
        } catch (JwtException | NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰의 유효성을 검증하는 메서드
     *
     * @param token 검증할 JWT
     * @return 유효할 경우 true 반환, 예외 발생 시 처리 필요
     */
    public boolean validateToken(String token) {
        // 잘못된 토큰으로 요청 시 서버 에러 반환 수정 예정
        Jwts.parser().verifyWith(secret).build().parseSignedClaims(token);

        return true;
    }

    /**
     * 토큰 만료 여부 확인을 위한 메서드
     * 토큰 만료 시 예외 반환
     *
     * @param token
     */
    public void isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();

            if (expiration.before(new Date())) {
                throw new BusinessException(ErrorCode.EXPIRED_TOKEN); // 401 Unauthorized
            }
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN); // 파싱 실패 시
        }
    }
}
