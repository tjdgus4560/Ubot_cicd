package com.autric.upbit.global.security.handler;

import com.autric.upbit.domain.member.entity.Member;
import com.autric.upbit.domain.oauth.service.JwtService;
import com.autric.upbit.global.config.properties.AppJwtProperties;
import com.autric.upbit.global.config.properties.AppOAuthProperties;
import com.autric.upbit.global.security.jwt.JwtProvider;
import com.autric.upbit.global.security.oauth2.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final JwtService jwtService;
    private final AppJwtProperties jwtProperties;
    private final AppOAuthProperties oauthProperties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        jwtService.save(member.getId(), refreshToken);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) jwtProperties.getRefreshExpired() / 1000);
        response.addCookie(refreshTokenCookie);

        response.sendRedirect(oauthProperties.getRedirectUrl());
    }
}
