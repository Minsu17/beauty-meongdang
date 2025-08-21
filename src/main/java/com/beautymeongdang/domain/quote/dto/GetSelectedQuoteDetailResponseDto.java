package com.beautymeongdang.domain.quote.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetSelectedQuoteDetailResponseDto {
    private String customerName;
    private String groomerName;
    private String shopName;
    private String shopLogo;
    private String address;
    private String phone;
    private String dogName;
    private String profileImage;
    private String dogBreed;
    private String dogWeight;
    private Integer dogAge;
    private String dogGender;
    private Boolean neutering;
    private Boolean experience;
    private String significant;
    private Long quoteId;
    private LocalDateTime beautyDate;
    private String requestContent;
    private String quoteContent;
    private Integer amount;
    private List<String> requestImage;
    private String paymentKey;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long requestId;

    @Builder
    public GetSelectedQuoteDetailResponseDto(
            String customerName,
            String groomerName,
            String shopName,
            String shopLogo,
            String address,
            String phone,
            String dogName,
            String profileImage,
            String dogBreed,
            String dogWeight,
            Integer dogAge,
            String dogGender,
            Boolean neutering,
            Boolean experience,
            String significant,
            Long quoteId,
            LocalDateTime beautyDate,
            String requestContent,
            String quoteContent,
            Integer amount,
            String paymentKey,
            BigDecimal latitude,
            BigDecimal longitude,
            Long requestId
    ) {
        this.customerName = customerName;
        this.groomerName = groomerName;
        this.shopName = shopName;
        this.shopLogo = shopLogo;
        this.address = address;
        this.phone = phone;
        this.dogName = dogName;
        this.profileImage = profileImage;
        this.dogBreed = dogBreed;
        this.dogWeight = dogWeight;
        this.dogAge = dogAge;
        this.dogGender = dogGender;
        this.neutering = neutering;
        this.experience = experience;
        this.significant = significant;
        this.quoteId = quoteId;
        this.beautyDate = beautyDate;
        this.requestContent = requestContent;
        this.quoteContent = quoteContent;
        this.amount = amount;
        this.paymentKey = paymentKey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.requestId = requestId;
    }
}
