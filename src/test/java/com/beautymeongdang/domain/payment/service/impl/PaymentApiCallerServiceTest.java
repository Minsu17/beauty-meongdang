package com.beautymeongdang.domain.payment.service.impl;

import com.beautymeongdang.domain.payment.dto.PaymentRequestDto;
import com.beautymeongdang.global.exception.handler.InternalServerException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentApiCallerServiceTest {

    @Autowired
    private PaymentApiCallerService paymentApiCallerService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private WebClient webClient;

    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.RequestBodySpec requestBodySpec;
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    private PaymentRequestDto testPaymentRequest;

    @BeforeEach
    void setUp() {
        // Mock 객체 초기화
        this.requestBodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        this.requestBodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        this.requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        this.responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        // 서킷 브레이커 상태 리셋
        circuitBreakerRegistry.circuitBreaker("tossPaymentService").reset();

        testPaymentRequest = PaymentRequestDto.builder()
                .paymentKey("test_payment_key")
                .orderId("test_order_id")
                .amount(10000)
                .quoteId(1L)
                .customerId(1L)
                .build();

        // WebClient의 Mock 동작 정의
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("서킷 브레이커 전체 생명주기(Closed -> Open -> Half-Open -> Closed) 테스트")
    void testCircuitBreakerFullLifecycle() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("tossPaymentService");

        // 0. 초기 상태 확인
        System.out.println("=== 0. 초기 상태 확인 ===");
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        System.out.println("✅ [상태 로그] 서킷 브레이커 초기 상태: " + circuitBreaker.getState());

        // 초기에 실패하도록 Mocking 설정
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new InternalServerException("API 호출 실패")));

        // 1. [CLOSED -> OPEN] 설정값(minimumNumberOfCalls=10)에 맞춰 10번 실패를 발생
        System.out.println("\n=== 1. OPEN 상태 전환 테스트 시작 (최소 호출 10회) ===");
        for (int i = 0; i < 10; i++) {
            assertThrows(InternalServerException.class, () ->
                    paymentApiCallerService.confirmTossPaymentWithRetryAndCircuitBreaker(testPaymentRequest)
            );
        }
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        System.out.println("✅ [상태 로그] 서킷 브레이커 상태: " + circuitBreaker.getState());


        // 2. [OPEN -> HALF-OPEN] 설정된 대기 시간(waitDurationInOpenState) 후 상태 전환 확인
        System.out.println("\n=== 2. HALF-OPEN 상태 전환 테스트 시작 ===");
        await()
                .atMost(Duration.ofSeconds(3)) // 테스트용 설정(1s)을 고려해 최대 3초 대기
                .until(() -> circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
        System.out.println("✅ [상태 로그] 서킷 브레이커 상태: " + circuitBreaker.getState());


        // 3. [HALF-OPEN -> CLOSED] 복구 시나리오 테스트
        System.out.println("\n=== 3. CLOSED 상태 복귀 테스트 시작 ===");

        // API가 성공하도록 Mocking 변경
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(Map.of("status", "DONE")));

        // 설정값(permittedNumberOfCallsInHalfOpenState=3)에 맞춰 3번의 성공 호출 실행
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() ->
                    paymentApiCallerService.confirmTossPaymentWithRetryAndCircuitBreaker(testPaymentRequest)
            );
        }

        // 최종 상태 확인
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        System.out.println("✅ [상태 로그] 서킷 브레이커 상태: " + circuitBreaker.getState());
    }
}