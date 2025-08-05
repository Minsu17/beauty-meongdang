package com.beautymeongdang.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationNotificationDto {
    private Long customerId;
    private Long groomerId;
    private String customerName;
    private String groomerNickname;
    private String dogName;
    private int amount;
    private String beautyDate;
}