package com.beautymeongdang.domain.notification.service;

import com.beautymeongdang.domain.payment.dto.ReservationNotificationDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReservationNotificationEvent extends ApplicationEvent {
    private final ReservationNotificationDto reservationDto;

    public ReservationNotificationEvent(Object source, ReservationNotificationDto reservationDto) {
        super(source);
        this.reservationDto = reservationDto;
    }
}