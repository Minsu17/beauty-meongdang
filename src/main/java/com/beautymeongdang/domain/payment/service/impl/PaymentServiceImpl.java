package com.beautymeongdang.domain.payment.service.impl;

import com.beautymeongdang.domain.notification.enums.NotificationType;
import com.beautymeongdang.domain.notification.service.NotificationService;
import com.beautymeongdang.domain.payment.dto.PaymentCancelRequestDto;
import com.beautymeongdang.domain.payment.dto.PaymentCancelResponseDto;
import com.beautymeongdang.domain.payment.dto.PaymentRequestDto;
import com.beautymeongdang.domain.payment.dto.PaymentResponseDto;
import com.beautymeongdang.domain.payment.entity.Payment;
import com.beautymeongdang.domain.payment.repository.PaymentRepository;
import com.beautymeongdang.domain.payment.service.PaymentService;
import com.beautymeongdang.domain.quote.entity.Quote;
import com.beautymeongdang.domain.quote.entity.QuoteRequest;
import com.beautymeongdang.domain.quote.entity.SelectedQuote;
import com.beautymeongdang.domain.quote.repository.QuoteRepository;
import com.beautymeongdang.domain.quote.repository.QuoteRequestRepository;
import com.beautymeongdang.domain.quote.repository.SelectedQuoteRepository;
import com.beautymeongdang.domain.shop.repository.ShopRepository;
import com.beautymeongdang.domain.user.entity.Customer;
import com.beautymeongdang.global.common.entity.CommonCode;
import com.beautymeongdang.global.common.repository.CommonCodeRepository;
import com.beautymeongdang.global.exception.handler.BadRequestException;
import com.beautymeongdang.global.exception.handler.InternalServerException;
import com.beautymeongdang.global.exception.handler.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final QuoteRequestRepository quoteRequestRepository;
    @Value("${toss.payments.secret.key}")
    private String secretKey;

    private final PaymentRepository paymentRepository;
    private final SelectedQuoteRepository selectedQuoteRepository;
    private final ShopRepository shopRepository;
    private final QuoteRepository quoteRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final NotificationService notificationService;
    private final PaymentApiCallerService paymentApiCallerService;

    public static final String PAYMENT_GROUP = "300"; // 결제 상태 그룹
    public static final String PAYMENT_COMPLETED = "020";    // 결제 완료
    public static final String PAYMENT_CANCELLED = "030";   // 결제 취소
    public static final String RESERVATION_COMPLETED = "010"; // 예약 완료
    public static final String RESERVATION_CANCELLED = "020"; // 예약 취소
    public static final String QUOTE_ACCEPT = "020";
    public static final String QUOTE_REQUEST_DEADLINE = "030";
    public static final String QUOTE_ALL_REQUEST = "010";

    // 결제 승인 요청 및 예약 완료
    @Override
    @Transactional
    public PaymentResponseDto confirmPayment(PaymentRequestDto request) {

        Quote quote = quoteRepository.findQuoteForPaymentById(request.getQuoteId())
                .orElseThrow(() -> NotFoundException.entityNotFound("견적 데이터"));

        Customer customer = quote.getDogId().getCustomerId();
        if (customer == null || !customer.getCustomerId().equals(request.getCustomerId())) {
            throw NotFoundException.entityNotFound("고객 데이터");
        }

        if (selectedQuoteRepository.findByQuoteId(quote) != null) {
            throw BadRequestException.invalidRequest("해당 견적서는 이미 예약되었습니다.");
        }

        SelectedQuote selectedQuotePay = selectedQuoteRepository.findByQuoteId(quote);
        if (selectedQuotePay != null && paymentRepository.existsBySelectedQuoteId(selectedQuotePay)) {
            throw BadRequestException.invalidRequest("이미 결제된 견적서입니다.");
        }

        Long groomerId = quote.getGroomerId().getGroomerId();
        String shopName = shopRepository.findByGroomerId(groomerId)
                .orElseThrow(() -> NotFoundException.entityNotFound("샵 정보"))
                .getShopName();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(secretKey, "");

        // API 호출
        Map<String, Object> response = paymentApiCallerService.confirmTossPaymentWithRetryAndCircuitBreaker(request, headers);

        if (response == null || response.get("approvedAt") == null) {
            throw InternalServerException.error("결제 승인 응답이 유효하지 않음");
        }

        OffsetDateTime approvedAtOffset = OffsetDateTime.parse(response.get("approvedAt").toString());
        LocalDateTime approvedAt = approvedAtOffset.toLocalDateTime();
        String method = response.get("method").toString();

        SelectedQuote selectedQuote = SelectedQuote.builder()
                .quoteId(quote)
                .customerId(customer)
                .status(RESERVATION_COMPLETED)
                .build();

        selectedQuote = selectedQuoteRepository.save(selectedQuote);

        // 견적서 상태 변경
        quote = Quote.builder()
                .quoteId(quote.getQuoteId())
                .requestId(quote.getRequestId())
                .groomerId(quote.getGroomerId())
                .dogId(quote.getDogId())
                .content(quote.getContent())
                .cost(quote.getCost())
                .beautyDate(quote.getBeautyDate())
                .status(QUOTE_ACCEPT)
                .build();

        quoteRepository.save(quote);

        // 견적서 요청 상태 변경 ( 전체 공고만 )
        QuoteRequest requestEntity = quote.getRequestId();

        if (QUOTE_ALL_REQUEST.equals(requestEntity.getRequestType())) {
            requestEntity = QuoteRequest.builder()
                    .requestId(requestEntity.getRequestId())
                    .dogId(requestEntity.getDogId())
                    .content(requestEntity.getContent())
                    .beautyDate(requestEntity.getBeautyDate())
                    .requestType(requestEntity.getRequestType())
                    .status(QUOTE_REQUEST_DEADLINE)
                    .build();

            quoteRequestRepository.save(requestEntity);
        }


        Payment payment = Payment.builder()
                .paymentKey(request.getPaymentKey())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .method(method)
                .status(PAYMENT_COMPLETED)
                .approvedAt(approvedAt)
                .paymentTitle(shopName)
                .selectedQuoteId(selectedQuote)
                .build();

        paymentRepository.save(payment);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formattedBeautyDate = quote.getBeautyDate().format(formatter);

        // 알림 메시지 생성
        CreateReserveNotification reserve = CreateReserveNotification(request, quote, formattedBeautyDate, customer);

        //알림 메시지 저장
        saveNotification(customer, NotificationType.RESERVATION, reserve.notificationMessageForCustomer(), quote, reserve.notificationMessageForGroomer());

        String statusName = commonCodeRepository.findByCodeAndGroupCode(payment.getStatus(), PAYMENT_GROUP)
                .map(CommonCode::getCommonName)
                .orElse("알 수 없는 상태");

        return PaymentResponseDto.builder()
                .paymentKey(request.getPaymentKey())
                .orderId(request.getOrderId())
                .status(statusName)
                .method(method)
                .approvedAt(approvedAtOffset)
                .amount(request.getAmount())
                .selectedQuoteId(selectedQuote.getSelectedQuoteId())
                .message("결제 승인 성공")
                .paymentTitle(shopName)
                .build();
    }

    private void saveNotification(Customer customer, NotificationType reservation, String reserve, Quote quote, String reserve1) {
        // 고객 알림 저장 (예약 알림)
        notificationService.saveNotification(
                customer.getUserId().getUserId(),
                "customer",
                reservation.getDescription(),
                reserve
        );

        // 미용사 알림 저장 (예약 알림)
        notificationService.saveNotification(
                quote.getGroomerId().getUserId().getUserId(),
                "groomer",
                reservation.getDescription(),
                reserve1
        );
    }

    private static CreateReserveNotification CreateReserveNotification(PaymentRequestDto request, Quote quote, String formattedBeautyDate, Customer customer) {
        String notificationMessageForCustomer = String.format(
                "예약이 완료되었습니다. 미용사: %s, 강아지: %s, 비용: %d원, 미용 날짜: %s",
                quote.getGroomerId().getUserId().getNickname(),
                quote.getDogId().getDogName(),
                request.getAmount(),
                formattedBeautyDate
        );

        String notificationMessageForGroomer = String.format(
                "예약이 완료되었습니다. 고객: %s, 강아지: %s, 비용: %d원, 미용 날짜: %s",
                customer.getUserId().getUserName(),
                quote.getDogId().getDogName(),
                request.getAmount(),
                formattedBeautyDate
        );
        CreateReserveNotification reserve = new CreateReserveNotification(notificationMessageForCustomer, notificationMessageForGroomer);
        return reserve;
    }

    private record CreateReserveNotification(String notificationMessageForCustomer, String notificationMessageForGroomer) {
    }


    // 결제 취소 및 예약 취소
    @Override
    @Transactional
    public PaymentCancelResponseDto cancelPayment(PaymentCancelRequestDto request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(secretKey, "");

        Map<String, Object> body = Map.of(
                "cancelReason", request.getCancelReason()
        );

        String url = "https://api.tosspayments.com/v1/payments/" + request.getPaymentKey() + "/cancel";

        try {
            // API 호출
            Map<String, Object> response = paymentApiCallerService.cancelTossPaymentWithRetryAndCircuitBreaker(url, headers, body);

            if (response != null) {

                Payment payment = paymentRepository.findByPaymentKey(request.getPaymentKey())
                        .orElseThrow(() -> NotFoundException.entityNotFound("결제 정보"));

                payment = payment.toBuilder()
                        .status(PAYMENT_CANCELLED)
                        .cancelReason(request.getCancelReason())
                        .build();

                paymentRepository.save(payment);

                SelectedQuote selectedQuote = payment.getSelectedQuoteId();
                selectedQuote = selectedQuote.updateStatus(RESERVATION_CANCELLED);
                selectedQuoteRepository.save(selectedQuote);

                // 예약 취소 알림 메시지 생성
                String notificationMessageForCustomer = String.format(
                        "예약이 취소되었습니다. 미용사: %s, 강아지: %s, 취소 비용: %d원, 취소 사유: %s",
                        selectedQuote.getQuoteId().getGroomerId().getUserId().getNickname(),
                        selectedQuote.getQuoteId().getDogId().getDogName(),
                        selectedQuote.getQuoteId().getCost(),
                        request.getCancelReason()

                );

                String notificationMessageForGroomer = String.format(
                        "예약이 취소되었습니다. 고객: %s, 강아지: %s, 취소 비용: %d원, 취소 사유: %s",
                        selectedQuote.getCustomerId().getUserId().getUserName(),
                        selectedQuote.getQuoteId().getDogId().getDogName(),
                        selectedQuote.getQuoteId().getCost(),
                        request.getCancelReason()
                );

                // 고객 알림 저장 (예약 취소 알림)
                saveNotification(selectedQuote.getCustomerId(), NotificationType.CANCELLATION, notificationMessageForCustomer, selectedQuote.getQuoteId(), notificationMessageForGroomer);

                String statusName = commonCodeRepository.findByCodeAndGroupCode(payment.getStatus(), PAYMENT_GROUP)
                        .map(CommonCode::getCommonName)
                        .orElse("알 수 없는 상태");

                return PaymentCancelResponseDto.builder()
                        .paymentKey(request.getPaymentKey())
                        .status(statusName)
                        .method(payment.getMethod())
                        .cancelReason(request.getCancelReason())
                        .selectedQuoteId(payment.getSelectedQuoteId().getSelectedQuoteId())
                        .message("결제 취소 성공")
                        .build();
            } else {
                throw InternalServerException.error("결제 취소 응답이 유효하지 않음");
            }
        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw InternalServerException.error(e.getMessage());
        }
    }

    // 결제 내역 조회
    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentDetail(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> NotFoundException.entityNotFound("결제 정보"));

        String statusName = commonCodeRepository.findByCodeAndGroupCode(payment.getStatus(), PAYMENT_GROUP)
                .map(CommonCode::getCommonName)
                .orElse("알 수 없는 상태");

        return PaymentResponseDto.builder()
                .paymentKey(payment.getPaymentKey())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(statusName)
                .method(payment.getMethod())
                .approvedAt(payment.getApprovedAt().atOffset(OffsetDateTime.now().getOffset())) // LocalDateTime -> OffsetDateTime
                .selectedQuoteId(payment.getSelectedQuoteId().getSelectedQuoteId())
                .paymentTitle(payment.getPaymentTitle())
                .message("결제 내역 조회 성공")
                .cancelReason(payment.getCancelReason())
                .build();
    }

    // 결제 물리적 삭제
    @Override
    @Transactional
    public void deleteExpiredLogicalDeletedPayments() {
        // 30일 이전 데이터를 삭제 기준으로 설정
        LocalDateTime deleteDay = LocalDateTime.now().minusDays(30);
        List<Payment> expiredPayments = paymentRepository.findAllByIsDeletedAndUpdatedAtBefore(deleteDay);

        // 물리적 삭제 실행
        paymentRepository.deleteAll(expiredPayments);
    }

}