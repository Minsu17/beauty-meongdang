package com.beautymeongdang.domain.quote.service.impl;

import com.beautymeongdang.domain.payment.entity.Payment;
import com.beautymeongdang.domain.quote.dto.GetCustomerSelectedQuoteResponseDto;
import com.beautymeongdang.domain.quote.dto.GetSelectedQuoteDetailResponseDto;
import com.beautymeongdang.domain.quote.dto.GetGroomerSelectedQuoteResponseDto;
import com.beautymeongdang.domain.quote.entity.QuoteRequestImage;
import com.beautymeongdang.domain.quote.entity.SelectedQuote;
import com.beautymeongdang.domain.quote.repository.QuoteRequestImageRepository;
import com.beautymeongdang.domain.quote.repository.SelectedQuoteRepository;
import com.beautymeongdang.domain.quote.service.SelectedQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SelectedQuoteServiceImpl implements SelectedQuoteService {

    private final SelectedQuoteRepository selectedQuoteRepository;
    private final QuoteRequestImageRepository quoteRequestImageRepository;

    @Override
    public List<GetCustomerSelectedQuoteResponseDto> getSelectedQuotesForCustomer(Long customerId) {
        return selectedQuoteRepository.findCustomerSelectedQuotes(customerId);
    }

    @Override
    public List<GetGroomerSelectedQuoteResponseDto> getSelectedQuotesForGroomer(Long groomerId) {
        return selectedQuoteRepository.findGroomerSelectedQuotes(groomerId);
    }

    @Override
    public GetSelectedQuoteDetailResponseDto getQuoteDetail(Long selectedQuoteId) {
        GetSelectedQuoteDetailResponseDto mainData = selectedQuoteRepository.findQuoteDetailById(selectedQuoteId);

        List<String> requestImages = quoteRequestImageRepository.findAllByRequestId(mainData.getRequestId())
                .stream()
                .map(QuoteRequestImage::getImageUrl)
                .toList();

        mainData.setRequestImage(requestImages);

        return mainData;
    }

    // 미용 완료 변경
    @Override
    public void updateStatusToCompletedIfPastBeautyDate() {
        List<SelectedQuote> quotesToUpdate = selectedQuoteRepository.findByStatusAndBeautyDateBefore("010", LocalDateTime.now());
        quotesToUpdate.forEach(selectedQuote -> {
            selectedQuote.updateStatus("030");
            selectedQuoteRepository.save(selectedQuote);
        });
    }

    // 선택된 견적서 물리적 삭제
    @Override
    @Transactional
    public void deleteExpiredLogicalDeletedSelectedQuotes() {
        // 30일 이전 데이터를 삭제 기준으로 설정
        LocalDateTime deleteDay = LocalDateTime.now().minusDays(30);
        List<SelectedQuote> expiredSelectedQuotes = selectedQuoteRepository.findAllByIsDeletedAndUpdatedAtBefore(deleteDay);

        // 물리적 삭제 실행
        selectedQuoteRepository.deleteAll(expiredSelectedQuotes);
    }
}