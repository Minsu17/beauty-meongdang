package com.beautymeongdang.domain.notification.service;

import com.beautymeongdang.domain.payment.dto.ReservationCancelNotificationDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReservationCancelNotificationEvent extends ApplicationEvent {
    private final ReservationCancelNotificationDto cancelDto;

    public ReservationCancelNotificationEvent(Object source, ReservationCancelNotificationDto cancelDto) {
        super(source);
        this.cancelDto = cancelDto;
    }
}