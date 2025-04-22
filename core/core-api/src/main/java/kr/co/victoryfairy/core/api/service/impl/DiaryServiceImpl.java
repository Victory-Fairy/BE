package kr.co.victoryfairy.core.api.service.impl;

import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
import kr.co.victoryfairy.storage.db.core.entity.*;
import kr.co.victoryfairy.storage.db.core.repository.*;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final MemberEntityRepository memberEntityRepository;
    private final TeamEntityRepository teamEntityRepository;

    // 일기 작성
    @Transactional
    public DiaryDomain.DiaryDto writeDiary(DiaryDomain.DiaryDto diaryDto){
        // 로그인한 회원 조회
        Long id = Optional.ofNullable(RequestUtils.getId())
                .orElseThrow(() -> new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH));

        MemberEntity member = memberEntityRepository.findById(id)
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // 응원팀 조회
        TeamEntity team = teamEntityRepository.findById(diaryDto.teamId())
                .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // 일기를 작성할 경기 조회
        GameMatchEntity gameMatch = gameMatchRepository.findById(diaryDto.gameMatchId())
                .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        // 일기 필수 입력값 저장
        DiaryEntity diaryEntity = DiaryEntity.builder()
                .member(member)
                .team(team)
                .teamName(diaryDto.teamName())
                .viewType(diaryDto.viewType())
                .gameMatch(gameMatch)
                .build();
        DiaryEntity savedDiaryEntity = diaryRepository.save(diaryEntity);

        // 선택 입력값인 음식 리스트가 비어있지 않는 경우
        if (!diaryDto.foodNameList().isEmpty()) {
            List<DiaryFoodEntity> foodList = new ArrayList<>();
            for (String food : diaryDto.foodNameList()) {
                DiaryFoodEntity diaryFoodEntity = DiaryFoodEntity.builder()
                        .diaryEntity(savedDiaryEntity)
                        .foodName(food)
                        .build();
                foodList.add(diaryFoodEntity);

            }
            diaryFoodRepository.saveAll(foodList);

        }

        // 선택 입력값인 기분 리스트가 비어있지 않는 경우
        if (!diaryDto.moodList().isEmpty()) {
            List<DiaryMoodEntity> moodList = new ArrayList<>();
            for (String mood : diaryDto.moodList()) {
                DiaryMoodEntity diaryMoodEntity = DiaryMoodEntity.builder()
                        .diaryEntity(savedDiaryEntity)
                        .mood(mood)
                        .build();
                moodList.add(diaryMoodEntity);

            }
            diaryMoodRepository.saveAll(moodList);

        }

        // 선택 입력값인 함께한 사람 리스트가 비어있지 않는 경우
        if (!diaryDto.partnerList().isEmpty()) {
            List<PartnerEntity> partnerEntityList = new ArrayList<>();
            for (DiaryDomain.PartnerDto partnerDto : diaryDto.partnerList()) {
                PartnerEntity partnerEntity = PartnerEntity.builder()
                        .diaryEntity(savedDiaryEntity)
                        .name(partnerDto.name())
                        .teamName(partnerDto.teamName())
                        .build();
                partnerEntityList.add(partnerEntity);

            }
            partnerRepository.saveAll(partnerEntityList);

        }

        // 파라미터로 받은 좌석
        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = diaryDto.seat();

        // 선택 입력값인 좌석이 비어있지 않는 경우
        if (diaryDtoSeat != null) {
            // 좌석 조회
            SeatEntity seatEntity = seatRepository.findById(diaryDtoSeat.seatId())
                    .orElseThrow(()-> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

            // 좌석 이용 내역 저장
            SeatUseHistoryEntity seatUseHistoryEntity = SeatUseHistoryEntity.builder()
                    .diaryEntity(savedDiaryEntity)
                    .seatEntity(seatEntity)
                    .seatBlock(diaryDtoSeat.seatBlock())
                    .seatRow(diaryDtoSeat.seatRow())
                    .seatNumber(diaryDtoSeat.seatNumber())
                    .build();
            SeatUseHistoryEntity savedSeatUseHistoryEntity = seatUseHistoryRepository.save(seatUseHistoryEntity);

            // 좌석 리뷰 저장
            List<SeatReviewEntity> reviewList = new ArrayList<>();
            for (String review : diaryDtoSeat.seatReview()) {
                SeatReviewEntity seatReviewEntity = SeatReviewEntity.builder()
                        .seatUseHistoryEntity(savedSeatUseHistoryEntity)
                        .seatReview(review)
                        .build();
                reviewList.add(seatReviewEntity);
            }
            seatReviewRepository.saveAll(reviewList);

        }


        return diaryDto;
    }

}
