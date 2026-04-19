package com.autric.upbit.global.config;

import com.autric.upbit.domain.member.repository.MemberRepository;
import com.autric.upbit.global.security.filter.JwtAuthenticationFilter;
import com.autric.upbit.global.security.filter.JwtExceptionFilter;
import com.autric.upbit.global.security.handler.OAuth2AuthenticationSuccessHandler;
import com.autric.upbit.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    // 사용자 정보를 후처리할 커스텀 서비스
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // URL 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/token", "/login/**", "/oauth2/**",
                                 "/upbit/markets", "/actuator/health",
                                "/css/**", "/js/**").permitAll() // 공개 허용
                        .anyRequest().authenticated() // 그 외에는 인증 필요
                )

                // 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService) // 사용자 정보 받아오는 커스텀 로직 등록
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                /*
                 * jwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 배치
                 * formLogin을 disable 했으므로 UsernamePasswordAuthenticationFilter는 동작하지 않음
                 */
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter(), JwtAuthenticationFilter.class);

        return http.build();  // 필터 체인 빌드 후 반환
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000",
                                                "https://ubot2.site",
                                                "https://www.ubot2.site"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtExceptionFilter jwtExceptionFilter() {
        return new JwtExceptionFilter();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, memberRepository);
    }
}

