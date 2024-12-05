package com.beautymeongdang.global.oauth2;

import com.beautymeongdang.global.login.entity.GoogleToken;
import com.beautymeongdang.global.login.entity.GoogleUserInfo;
import com.beautymeongdang.global.login.entity.KakaoToken;
import com.beautymeongdang.global.login.entity.KakaoUserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthorizationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    public KakaoToken getKakaoAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        // 요청 파라미터 로깅
        log.info("login-log Kakao token request parameters - clientId: {}, clientSecret: {}, redirectUri: {}", clientId, clientSecret, redirectUri);

        // 요청 파라미터 구성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        // 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 객체 생성
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // Kakao 토큰 발급 요청 보내기
            ResponseEntity<KakaoToken> response = restTemplate.postForEntity(
                    tokenUrl,
                    request,
                    KakaoToken.class
            );

            // 토큰 발급 성공 로그 남기기
            log.info("login-log ✅ 카카오 토큰 발급 성공");

            // 응답 본문에서 토큰 정보 반환
            return response.getBody();
        } catch (Exception e) {
            // 토큰 발급 실패 로그 남기기
            log.error("login-log 카카오 토큰 요청 실패", e);

            // 예외 던지기
            throw new RuntimeException("카카오 토큰 발급 실패", e);
        }
    }

    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        log.info("login-log 👤 카카오 사용자 정보 요청 시작");
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            log.info("login-log📄 카카오 응답 데이터: {}", response.getBody());
            return KakaoUserInfo.builder()
                    .id(jsonNode.get("id").asLong())
                    .email(jsonNode.get("kakao_account").get("email").asText())
                    .name(jsonNode.get("properties").get("nickname").asText())
                    .build();
        } catch (Exception e) {
            log.error("login-log 카카오 사용자 정보 요청 실패", e);
            throw new RuntimeException("login-log 카카오 사용자 정보 조회 실패", e);
        }
    }
    public GoogleToken getGoogleAccessToken(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // 요청 파라미터 로깅
        log.info("login-log Google token request parameters - clientId: {}, clientSecret: {}, redirectUri: {}",
                googleClientId, googleClientSecret, googleRedirectUri);

        // 요청 파라미터 구성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", code);

        // 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 객체 생성
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // Google 토큰 발급 요청 보내기
            ResponseEntity<GoogleToken> response = restTemplate.postForEntity(
                    tokenUrl,
                    request,
                    GoogleToken.class
            );

            // 토큰 발급 성공 로그 남기기
            log.info("login-log ✅ 구글 토큰 발급 성공");

            // 응답 본문에서 토큰 정보 반환
            return response.getBody();
        } catch (Exception e) {
            // 토큰 발급 실패 로그 남기기
            log.error("login-log 구글 토큰 요청 실패", e);

            // 예외 던지기
            throw new RuntimeException("구글 토큰 발급 실패", e);
        }
    }

    public GoogleUserInfo getGoogleUserInfo(String accessToken) {
        log.info("login-log 👤 구글 사용자 정보 요청 시작");
        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            log.info("login-log📄 구글 응답 데이터: {}", response.getBody());
            return GoogleUserInfo.builder()
                    .id(jsonNode.get("sub").asText())
                    .email(jsonNode.get("email").asText())
                    .name(jsonNode.get("name").asText())
                    .profileImage(jsonNode.get("picture").asText())
                    .emailVerified(jsonNode.get("email_verified").asBoolean())
                    .locale(jsonNode.get("locale").asText())
                    .build();
        } catch (Exception e) {
            log.error("login-log 구글 사용자 정보 요청 실패", e);
            throw new RuntimeException("login-log 구글 사용자 정보 조회 실패", e);
        }
    }
}