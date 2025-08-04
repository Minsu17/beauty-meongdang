package com.beautymeongdang.global.config;

import com.beautymeongdang.global.login.service.impl.CustomOAuth2UserServiceImpl;
import com.beautymeongdang.domain.user.repository.UserRepository;
import com.beautymeongdang.global.jwt.JWTFilter;
import com.beautymeongdang.global.jwt.JWTUtil;
import com.beautymeongdang.global.jwt.JwtProvider;
import com.beautymeongdang.global.oauth2.CustomSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserServiceImpl customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // JWT 필터 인스턴스를 한 번만 생성
        JWTFilter jwtFilter = new JWTFilter(jwtUtil, userRepository, jwtProvider);

        http
                // CORS 설정
                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))

                // CSRF 비활성
                .csrf((auth) -> auth.disable())

                // 기본 로그인 방식 비활성화
                .formLogin((auth) -> auth.disable())
                .httpBasic((auth) -> auth.disable())
//                .oauth2Login((auth) -> auth.disable()) // 커스텀 방식(프론트 인가 코드)을 사용하기 떄문에 사용 X
                .oauth2Login(oauth2 -> oauth2           // 자동인증 방식 백앤드 로컬 테스트용
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(customSuccessHandler)
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")))

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/login.html")
                        .deleteCookies("JSESSIONID", "refreshToken")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .permitAll()
                )

                // URL 접근 권한 설정
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/api/users/register/**").authenticated()  // JWT 헤더에 인증이 필요함
                        // 인증이 필요없는 public 접근 경로
                        .requestMatchers(
                                "/**",
                                "/api/users/register/**",
                                "/login/oauth2/code/**",
                                "/selectRole.html",
                                "/login.html",
                                "/InfoRequired.jsx",
                                "/login.jsx",
                                "/index.html",
                                "/index1.html",
                                "/login/**",
                                "/api/auth/**",
                                "/oauth2/**",
                                // 프론트엔드 라우트들
                                "/",                    // 루트 경로
                                "/selectRole",         // 역할 선택 페이지
                                "/login",             // 로그인 페이지
                                "/oauth2/**"          // OAuth2 관련 모든 경로
                        ).permitAll()
                        // API 및 Swagger 관련 경로
                        .requestMatchers(
                                "/api/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/configuration/ui",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )


                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT 필터 추가 (한 번만 추가)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 설정
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",    // 프론트엔드 로컬
                "http://localhost:8081",    // 백엔드 로컬
                "https://beautymeongdang.com",
                "https://www.beautymeongdang.com",
                "https://beautymeongdang.vercel.app"
        ));

        // 허용할 HTTP 메서드 설정
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // 허용할 헤더 설정
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // 자격증명 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}