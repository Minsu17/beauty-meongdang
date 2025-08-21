package com.beautymeongdang.domain.quote.repository;

import com.beautymeongdang.domain.dog.entity.Dog;
import com.beautymeongdang.domain.payment.entity.Payment;
import com.beautymeongdang.domain.quote.dto.GetCustomerSelectedQuoteResponseDto;
import com.beautymeongdang.domain.quote.dto.GetSelectedQuoteDetailResponseDto;
import com.beautymeongdang.domain.quote.dto.GetGroomerSelectedQuoteResponseDto;
import com.beautymeongdang.domain.quote.entity.Quote;
import com.beautymeongdang.domain.quote.entity.SelectedQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SelectedQuoteRepository extends JpaRepository<SelectedQuote, Long> {

    @Query("SELECT new com.beautymeongdang.domain.quote.dto.GetCustomerSelectedQuoteResponseDto(" +
            "sq.selectedQuoteId, q.quoteId, g.groomerId,d.profileImage, s.shopName, " +
            "g.userId.nickname, q.beautyDate, d.dogName, " +
            "cc.commonName, " +
            "CASE WHEN r.selectedQuoteId.selectedQuoteId IS NOT NULL THEN true ELSE false END) " +
            "FROM SelectedQuote sq " +
            "JOIN sq.quoteId q " +
            "JOIN q.dogId d " +
            "JOIN q.groomerId g " +
            "JOIN Shop s ON s.groomerId = g " +
            "JOIN CommonCode cc ON cc.id.codeId = sq.status AND cc.id.groupId = '250' " +
            "LEFT JOIN Reviews r ON r.selectedQuoteId.selectedQuoteId = sq.selectedQuoteId " +
            "WHERE sq.customerId.customerId = :customerId AND sq.isDeleted = false")
    List<GetCustomerSelectedQuoteResponseDto> findCustomerSelectedQuotes(@Param("customerId") Long customerId);


    @Query("SELECT new com.beautymeongdang.domain.quote.dto.GetGroomerSelectedQuoteResponseDto(" +
            "sq.selectedQuoteId, q.quoteId, d.profileImage, c.userId.userName, " +
            "c.userId.nickname, c.userId.phone, d.dogName, q.beautyDate, " +
            "statusCode.commonName, breedCode.commonName) " +
            "FROM SelectedQuote sq " +
            "JOIN sq.quoteId q " +
            "JOIN sq.customerId c " +
            "JOIN q.dogId d " +
            "JOIN CommonCode statusCode ON statusCode.id.codeId = sq.status AND statusCode.id.groupId = '250' " +
            "JOIN CommonCode breedCode ON breedCode.id.codeId = d.dogBreed AND breedCode.id.groupId = '400' " +
            "WHERE q.groomerId.groomerId = :groomerId AND sq.isDeleted = false")
    List<GetGroomerSelectedQuoteResponseDto> findGroomerSelectedQuotes(@Param("groomerId") Long groomerId);

    @Query("SELECT new com.beautymeongdang.domain.quote.dto.GetSelectedQuoteDetailResponseDto(" +
            "c.userId.userName, g.userId.nickname, s.shopName, s.imageUrl, s.address, g.userId.phone, " +
            "d.dogName, d.profileImage, " +
            "cc.commonName, " +
            "d.dogWeight, d.dogAge, " +
            "CAST(d.dogGender AS string), " +
            "d.neutering, d.experience, d.significant, " +
            "q.quoteId, q.beautyDate, qr.content, q.content, q.cost, p.paymentKey, s.latitude, s.longitude, " +
            "qr.requestId) " +
            "FROM SelectedQuote sq " +
            "JOIN sq.quoteId q " +
            "JOIN q.requestId qr " +
            "JOIN q.dogId d " +
            "JOIN q.groomerId g " +
            "JOIN Shop s ON s.groomerId = g " +
            "JOIN sq.customerId c " +
            "JOIN Payment p ON p.selectedQuoteId = sq " +
            "JOIN CommonCode cc ON cc.id.codeId = d.dogBreed AND cc.id.groupId = '400'" +
            "WHERE sq.selectedQuoteId = :selectedQuoteId AND sq.isDeleted = false")
    GetSelectedQuoteDetailResponseDto findQuoteDetailById(@Param("selectedQuoteId") Long selectedQuoteId);


    // 미용사 메인 페이지 - 오늘의 예약 건수 조회
    @Query(value = """
        SELECT COUNT(sq) as todayReservation
        FROM SelectedQuote sq
        JOIN sq.quoteId q
        WHERE sq.status = '010'
          AND sq.isDeleted = false
          AND q.groomerId.groomerId = :groomerId
          AND FUNCTION('DATE', q.beautyDate) = FUNCTION('DATE', :todayDate)
    """)
    Integer countTodayReservations(@Param("groomerId") Long groomerId, @Param("todayDate") LocalDateTime todayDate);

    // 미용사 마이페이지 - 미용완료 건수
    @Query(value = """
        SELECT COUNT(sq) as completedServices
        FROM SelectedQuote sq
        JOIN sq.quoteId q
        WHERE sq.status = '030'
          AND sq.isDeleted = false
          AND q.groomerId.groomerId = :groomerId
    """)
    Integer countCompletedServices(@Param("groomerId") Long groomerId);

    // 미용사 마이페이지 - 확정된 예약 건수
    @Query(value = """
        SELECT COUNT(sq) as confirmedReservations
        FROM SelectedQuote sq
        JOIN sq.quoteId q
        WHERE sq.status = '010'
          AND sq.isDeleted = false
          AND q.groomerId.groomerId = :groomerId
    """)
    Integer countConfirmedReservations(@Param("groomerId") Long groomerId);

    @Query("SELECT sq FROM SelectedQuote sq WHERE sq.customerId.customerId = :customerId AND sq.isDeleted = false")
    List<SelectedQuote> findAllByCustomerId(@Param("customerId") Long customerId);

    // 미용사 프로필 논리적 삭제
    SelectedQuote findByQuoteId(Quote quote);

    // 반려견 프로필 논리적 삭제
    List<SelectedQuote> findAllByQuoteIdDogId(Dog dog);

    // 예약완료 수
    @Query("SELECT COUNT(sq) FROM SelectedQuote sq " +
            "JOIN CommonCode cc ON cc.id.codeId = sq.status AND cc.id.groupId = '250' " +
            "WHERE sq.customerId.customerId = :customerId " +
            "AND sq.status = '010' " +
            "AND sq.isDeleted = false")
    Integer countConfirmedReservationsByCustomerId(@Param("customerId") Long customerId);

    // 미용 완료 수
    @Query("SELECT COUNT(sq) FROM SelectedQuote sq " +
            "JOIN CommonCode cc ON cc.id.codeId = sq.status AND cc.id.groupId = '250' " +
            "WHERE sq.customerId.customerId = :customerId " +
            "AND sq.status = '030' " +
            "AND sq.isDeleted = false")
    Integer countCompletedServicesByCustomerId(@Param("customerId") Long customerId);

    // 미용 완료 상태 변경
    @Query("SELECT sq FROM SelectedQuote sq " +
            "JOIN sq.quoteId q " +
            "WHERE sq.status = :status AND q.beautyDate < :currentDate AND sq.isDeleted = false")
    List<SelectedQuote> findByStatusAndBeautyDateBefore(@Param("status") String status, @Param("currentDate") LocalDateTime currentDate);

    // 선택된 견적서 물리적 삭제
    @Query("""
    SELECT s
    FROM SelectedQuote s
    WHERE s.isDeleted = true
      AND s.updatedAt < :deleteDay
    """)
    List<SelectedQuote> findAllByIsDeletedAndUpdatedAtBefore(@Param("deleteDay") LocalDateTime deleteDay);
}
