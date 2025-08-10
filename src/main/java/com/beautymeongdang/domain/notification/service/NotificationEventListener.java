package com.beautymeongdang.domain.notification.service;

import com.beautymeongdang.domain.notification.enums.NotificationType;
import com.beautymeongdang.domain.payment.dto.ReservationCancelNotificationDto;
import com.beautymeongdang.domain.payment.dto.ReservationNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 결제 트랜잭션 커밋 후 호출
    public void handleReservationNotificationEvent(ReservationNotificationEvent event) {
        log.info("예약 알림 이벤트 처리 시작 (비동기): 고객 ID={}, 미용사 ID={}",
                event.getReservationDto().getCustomerId(),
                event.getReservationDto().getGroomerId());

        ReservationNotificationDto dto = event.getReservationDto();

        // 메시지 생성
        String customerMessage = String.format(
                "예약이 완료되었습니다. 미용사: %s, 강아지: %s, 비용: %d원, 미용 날짜: %s",
                dto.getGroomerNickname(),
                dto.getDogName(),
                dto.getAmount(),
                dto.getBeautyDate()
        );

        String groomerMessage = String.format(
                "예약이 완료되었습니다. 고객: %s, 강아지: %s, 비용: %d원, 미용 날짜: %s",
                dto.getCustomerName(),
                dto.getDogName(),
                dto.getAmount(),
                dto.getBeautyDate()
        );

        // 알림 저장 로직 호출
        notificationService.saveNotification(
                dto.getCustomerId(),
                "customer",
                NotificationType.RESERVATION.getDescription(),
                customerMessage
        );

        notificationService.saveNotification(
                dto.getGroomerId(),
                "groomer",
                NotificationType.RESERVATION.getDescription(),
                groomerMessage
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReservationCancelNotificationEvent(ReservationCancelNotificationEvent event) {
        log.info("예약 취소 알림 이벤트 처리 시작 (비동기): 고객 ID={}, 미용사 ID={}",
                event.getCancelDto().getCustomerId(),
                event.getCancelDto().getGroomerId());

        ReservationCancelNotificationDto dto = event.getCancelDto();

        String customerMessage = String.format(
                "예약이 취소되었습니다. 미용사: %s, 강아지: %s, 취소 비용: %d원, 취소 사유: %s",
                dto.getGroomerNickname(),
                dto.getDogName(),
                dto.getCost(),
                dto.getCancelReason()
        );

        String groomerMessage = String.format(
                "예약이 취소되었습니다. 고객: %s, 강아지: %s, 취소 비용: %d원, 취소 사유: %s",
                dto.getCustomerName(),
                dto.getDogName(),
                dto.getCost(),
                dto.getCancelReason()
        );

        notificationService.saveNotification(
                dto.getCustomerId(),
                "customer",
                NotificationType.CANCELLATION.getDescription(),
                customerMessage
        );

        notificationService.saveNotification(
                dto.getGroomerId(),
                "groomer",
                NotificationType.CANCELLATION.getDescription(),
                groomerMessage
        );
    }
}