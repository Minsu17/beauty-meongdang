package com.beautymeongdang.domain.payment.dto;

import com.beautymeongdang.domain.quote.entity.Quote;
import com.beautymeongdang.domain.user.entity.Customer;
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
    private Long amount;
    private String beautyDate;

    public static ReservationNotificationDto of(Quote quote, Customer customer, Long paymentAmount, String formattedBeautyDate) {
        return ReservationNotificationDto.builder()
                .customerId(customer.getUserId().getUserId())
                .groomerId(quote.getGroomerId().getUserId().getUserId())
                .customerName(customer.getUserId().getUserName())
                .groomerNickname(quote.getGroomerId().getUserId().getNickname())
                .dogName(quote.getDogId().getDogName())
                .amount(paymentAmount)
                .beautyDate(formattedBeautyDate)
                .build();
    }
}