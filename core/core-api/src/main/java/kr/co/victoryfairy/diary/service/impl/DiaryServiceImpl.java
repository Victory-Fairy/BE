package kr.co.victoryfairy.diary.service.impl;

import kr.co.victoryfairy.diary.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.dto.DiaryDto;
import kr.co.victoryfairy.storage.db.core.dto.PartnerDto;
import kr.co.victoryfairy.storage.db.core.dto.SeatUseHistoryDto;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final SeatRepository seatRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;
    private final SeatReviewRepository seatReviewRepository;
    private final PartnerRepository partnerRepository;
    private final DiaryMoodRepository diaryMoodRepository;
    private final GameMatchEntityRepository gameMatchRepository;

    // 일기 작성
    public DiaryDto writeDiary(DiaryDto diaryDto){
        // 일기를 작성할 경기 조회
        GameMatchEntity gameMatch = gameMatchRepository.findById(diaryDto.getGameMatchId())
                .orElseThrow();

        // 일기 필수 입력값 저장
        Diary diary = Diary.builder()
                .teamName(diaryDto.getTeamName())
                .viewType(diaryDto.getViewType())
                .gameMatch(gameMatch)
                .build();
        Diary savedDiary = diaryRepository.save(diary);

        // 선택 입력값인 음식 리스트가 비어있지 않는 경우
        if (!diaryDto.getFoodNameList().isEmpty()) {
            List<DiaryFood> foodList = new ArrayList<>();
            for (String food : diaryDto.getFoodNameList()) {
                DiaryFood diaryFood = DiaryFood.builder()
                        .diary(savedDiary)
                        .foodName(food)
                        .build();
                foodList.add(diaryFood);

            }
            diaryFoodRepository.saveAll(foodList);

        }

        // 선택 입력값인 기분 리스트가 비어있지 않는 경우
        if (!diaryDto.getMoodList().isEmpty()) {
            List<DiaryMood> moodList = new ArrayList<>();
            for (String mood : diaryDto.getMoodList()) {
                DiaryMood diaryMood = DiaryMood.builder()
                        .diary(savedDiary)
                        .mood(mood)
                        .build();
                moodList.add(diaryMood);

            }
            diaryMoodRepository.saveAll(moodList);

        }

        // 선택 입력값인 함께한 사람 리스트가 비어있지 않는 경우
        if (!diaryDto.getPartnerList().isEmpty()) {
            List<Partner> partnerList = new ArrayList<>();
            for (PartnerDto partnerDto : diaryDto.getPartnerList()) {
                Partner partner = Partner.builder()
                        .diary(savedDiary)
                        .name(partnerDto.getName())
                        .teamName(partnerDto.getTeamName())
                        .build();
                partnerList.add(partner);

            }
            partnerRepository.saveAll(partnerList);

        }

        // 파라미터로 받은 좌석
        SeatUseHistoryDto diaryDtoSeat = diaryDto.getSeat();

        // 선택 입력값인 좌석이 비어있지 않는 경우
        if (diaryDtoSeat != null) {
            // 좌석 조회
            Seat seat = seatRepository.findById(diaryDtoSeat.getSeatId()).orElseThrow();

            // 좌석 이용 내역 저장
            SeatUseHistory seatUseHistory = SeatUseHistory.builder()
                    .diary(savedDiary)
                    .seat(seat)
                    .seatBlock(diaryDtoSeat.getSeatBlock())
                    .seatRow(diaryDtoSeat.getSeatRow())
                    .seatNumber(diaryDtoSeat.getSeatNumber())
                    .build();
            SeatUseHistory savedSeatUseHistory = seatUseHistoryRepository.save(seatUseHistory);

            // 좌석 리뷰 저장
            List<SeatReview> reviewList = new ArrayList<>();
            for (String review : diaryDtoSeat.getSeatReview()) {
                SeatReview seatReview = SeatReview.builder()
                        .seatUseHistory(savedSeatUseHistory)
                        .seatReview(review)
                        .build();
                reviewList.add(seatReview);
            }
            seatReviewRepository.saveAll(reviewList);

        }

        return diaryDto;
    }

}
