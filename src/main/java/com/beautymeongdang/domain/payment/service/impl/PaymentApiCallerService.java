package com.beautymeongdang.domain.payment.service.impl;

import com.beautymeongdang.domain.payment.dto.PaymentRequestDto;
import com.beautymeongdang.global.exception.handler.InternalServerException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApiCallerService {

    private final WebClient webClient;

    private static final String TOSS_PAYMENTS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            )
    )
    @CircuitBreaker(name = "tossPaymentService", fallbackMethod = "fallbackForPaymentConfirm")
    public Map<String, Object> confirmTossPaymentWithRetryAndCircuitBreaker(PaymentRequestDto request, HttpHeaders headers) {
        Map<String, Object> body = Map.of(
                "paymentKey", request.getPaymentKey(),
                "orderId", request.getOrderId(),
                "amount", request.getAmount()
        );
        // API 호출
        Map<String, Object> response = webClient.post()
                .uri(TOSS_PAYMENTS_CONFIRM_URL)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        return response;
    }

    private Map<String, Object> fallbackForPaymentConfirm(PaymentRequestDto request, HttpHeaders headers, Throwable t) {
        log.error("결제 승인 API 호출 최종 실패. 서킷 브레이커 Open. 주문 ID: {}, 오류: {}", request.getOrderId(), t.getMessage());
        throw InternalServerException.error("현재 결제 시스템에 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            )
    )
    @CircuitBreaker(name = "tossPaymentService", fallbackMethod = "fallbackForPaymentCancel")
    public Map<String, Object> cancelTossPaymentWithRetryAndCircuitBreaker(String url, HttpHeaders headers, Map<String, Object> body) {
        Map<String, Object> response = webClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        return response;
    }

    private Map<String, Object> fallbackForPaymentCancel(String url, HttpHeaders headers, Map<String, Object> body, Throwable t) {
        log.error("결제 취소 API 호출 최종 실패. 서킷 브레이커 Open. URL: {}, 오류: {}", url, t.getMessage());
        throw InternalServerException.error("현재 결제 취소 시스템에 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }
}