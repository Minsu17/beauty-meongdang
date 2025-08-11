package com.beautymeongdang.domain.payment.dto;

import com.beautymeongdang.domain.quote.entity.SelectedQuote;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationCancelNotificationDto {
    private final Long customerId;
    private final Long groomerId;
    private final String customerName;
    private final String groomerNickname;
    private final String dogName;
    private final Long cost;
    private final String cancelReason;

    public static ReservationCancelNotificationDto of(SelectedQuote selectedQuote, String cancelReason) {
        return ReservationCancelNotificationDto.builder()
                .customerId(selectedQuote.getCustomerId().getUserId().getUserId())
                .groomerId(selectedQuote.getQuoteId().getGroomerId().getUserId().getUserId())
                .customerName(selectedQuote.getCustomerId().getUserId().getUserName())
                .groomerNickname(selectedQuote.getQuoteId().getGroomerId().getUserId().getNickname())
                .dogName(selectedQuote.getQuoteId().getDogId().getDogName())
                .cost(selectedQuote.getQuoteId().getCost().longValue())
                .cancelReason(cancelReason)
                .build();
    }
}