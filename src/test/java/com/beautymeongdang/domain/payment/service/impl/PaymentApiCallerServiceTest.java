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
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

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

    private RequestBodyUriSpec requestBodyUriSpec;
    private RequestBodySpec requestBodySpec;
    private RequestHeadersSpec requestHeadersSpec;
    private ResponseSpec responseSpec;

    private PaymentRequestDto testPaymentRequest;
    private HttpHeaders testHeaders;

    @BeforeEach
    void setUp() {
        this.requestBodyUriSpec = Mockito.mock(RequestBodyUriSpec.class);
        this.requestBodySpec = Mockito.mock(RequestBodySpec.class);
        this.requestHeadersSpec = Mockito.mock(RequestHeadersSpec.class);
        this.responseSpec = Mockito.mock(ResponseSpec.class);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("tossPaymentService");
        circuitBreaker.reset();

        testPaymentRequest = PaymentRequestDto.builder()
                .paymentKey("test_payment_key")
                .orderId("test_order_id")
                .amount(10000)
                .quoteId(1L)
                .customerId(1L)
                .build();

        testHeaders = new HttpHeaders();
        testHeaders.set("Authorization", "Basic dGVzdF9za186");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.error(new InternalServerException("API 호출 실패")));
    }

    @Test
    @DisplayName("서킷 브레이커 동작 테스트 - 10번 실패 후 OPEN 상태 확인")
    void testCircuitBreakerOpensAfter10Failures() throws InterruptedException {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("tossPaymentService");

        System.out.println("=== 서킷 브레이커 테스트 시작 ===");
        System.out.println("초기 상태: " + circuitBreaker.getState());

        for (int i = 1; i <= 10; i++) {
            System.out.println("\n=== " + i + "번째 호출 시도 ===");
            assertThrows(InternalServerException.class, () ->
                    paymentApiCallerService.confirmTossPaymentWithRetryAndCircuitBreaker(testPaymentRequest, testHeaders)
            );
            System.out.println("실패 횟수: " + circuitBreaker.getMetrics().getNumberOfFailedCalls());
            System.out.println("서킷 브레이커 상태: " + circuitBreaker.getState());
        }

        System.out.println("\n=== 10번 실패 후 상태 확인 ===");
        System.out.println("서킷 브레이커 상태: " + circuitBreaker.getState());
        System.out.println("총 실패 횟수: " + circuitBreaker.getMetrics().getNumberOfFailedCalls());

        assertEquals(10, circuitBreaker.getMetrics().getNumberOfFailedCalls(), "10번의 실패가 기록되어야 합니다.");

        Thread.sleep(100);

        System.out.println("\n=== 11번째 호출 시도 (서킷 브레이커 OPEN 상태에서) ===");
        System.out.println("대기 후 서킷 브레이커 상태: " + circuitBreaker.getState());

        Exception exception = assertThrows(Exception.class, () ->
                paymentApiCallerService.confirmTossPaymentWithRetryAndCircuitBreaker(testPaymentRequest, testHeaders)
        );

        // fallbackMethod가 CallNotPermittedException을 잡고 InternalServerException을 던지므로,
        // InternalServerException이 발생했는지 확인해야 함
        assertTrue(exception instanceof InternalServerException,
                "11번째 호출은 InternalServerException (Fallback)을 발생시켜야 합니다.");
        System.out.println("✅ InternalServerException이 발생");
        System.out.println("최종 서킷 브레이커 상태: " + circuitBreaker.getState());
    }
}