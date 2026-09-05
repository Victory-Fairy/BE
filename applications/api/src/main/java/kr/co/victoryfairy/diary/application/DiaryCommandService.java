package kr.co.victoryfairy.diary.application;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.shared.application.model.CommonDto;
import kr.co.victoryfairy.diary.application.DiaryFoodDomainService;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.diary.application.PartnerDomainService;
import kr.co.victoryfairy.diary.presentation.DiaryDomain;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.GameMatchEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.game.infrastructure.persistence.entity.SeatEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.SeatUseHistoryEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.GameMatchRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.SeatRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.game.infrastructure.persistence.repository.TeamRepository;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DiaryCommandService {

    private final DiaryRepository diaryRepository;

    private final SeatRepository seatRepository;

    private final SeatUseHistoryRepository seatUseHistoryRepository;

    private final SeatReviewRepository seatReviewRepository;

    private final GameMatchRepository gameMatchRepository;

    private final GameRecordRepository gameRecordRepository;

    private final MemberRepository memberRepository;

    private final TeamRepository teamRepository;

    private final FileReferenceService fileRefDomainService;

    private final DiaryFoodDomainService diaryFoodDomainService;

    private final PartnerDomainService partnerDomainService;

    private final GameRecordDomainService gameRecordDomainService;

    @Transactional
    @DistributedLock(value = LockName.DIARY_WRITE, key = "#memberId + '_' + #diaryDto.gameMatchId()")
    public DiaryDomain.WriteResponse writeDiary(Long memberId, DiaryDomain.WriteRequest diaryDto) {
        MemberEntity member = memberRepository.findById(Objects.requireNonNull(memberId))
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        GameMatchEntity gameMatchEntity = gameMatchRepository.findDiaryWriteById(diaryDto.gameMatchId())
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(diaryDto.teamId())
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        if (diaryRepository.findByMemberAndGameMatchEntity(member, gameMatchEntity) != null) {
            throw new CustomException(HttpStatus.CONFLICT, MessageEnum.Data.FAIL_DUPLICATE);
        }

        DiaryEntity diaryEntity = DiaryEntity.builder()
            .member(member)
            .teamName(teamEntity.getName())
            .teamEntity(teamEntity)
            .viewType(diaryDto.viewType())
            .gameMatchEntity(gameMatchEntity)
            .weatherType(diaryDto.weather())
            .moodType(diaryDto.mood())
            .content(diaryDto.content())
            .build();
        diaryRepository.save(diaryEntity);

        fileRefDomainService.saveFileRefs(RefType.DIARY, diaryEntity.getId(), diaryDto.fileId());
        diaryFoodDomainService.saveFoods(RefType.DIARY, diaryEntity.getId(), diaryDto.foodNameList());
        partnerDomainService.savePartners(RefType.DIARY, diaryEntity.getId(),
                toPartnerSaveRequests(diaryDto.partnerList()));

        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = diaryDto.seat();
        if (diaryDtoSeat != null) {
            SeatEntity seatEntity = null;
            if (diaryDtoSeat.id() != null) {
                seatEntity = seatRepository.findById(diaryDtoSeat.id()).orElse(null);
            }

            SeatUseHistoryEntity seatUseHistoryEntity = SeatUseHistoryEntity.builder()
                .diaryEntity(diaryEntity)
                .seatEntity(seatEntity)
                .seatName(diaryDtoSeat.name())
                .build();
            seatUseHistoryRepository.save(seatUseHistoryEntity);
        }

        gameRecordDomainService.record(diaryEntity);

        return new DiaryDomain.WriteResponse(diaryEntity.getId());
    }

    @Transactional
    public void updateDiary(Long diaryId, DiaryDomain.UpdateRequest request) {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var teamEntity = teamRepository.findById(request.teamId())
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var gameRecordEntity = gameRecordRepository.findByMemberAndDiaryEntityId(member, diaryId);

        diaryEntity.updateDiary(teamEntity.getName(), teamEntity, request.viewType(), request.mood(), request.weather(),
                request.content());
        diaryRepository.save(diaryEntity);

        fileRefDomainService.replaceFileRefs(RefType.DIARY, diaryId, request.fileId());
        diaryFoodDomainService.replaceFoods(RefType.DIARY, diaryId, request.foodNameList());
        partnerDomainService.replacePartners(RefType.DIARY, diaryId, toPartnerSaveRequests(request.partnerList()));

        DiaryDomain.SeatUseHistoryDto diaryDtoSeat = request.seat();
        if (diaryDtoSeat != null) {
            var previousSeatUseHistory = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
            var previousSeatReviews = seatReviewRepository.findBySeatUseHistoryEntity(previousSeatUseHistory);

            if (!previousSeatReviews.isEmpty()) {
                seatReviewRepository.deleteAll(previousSeatReviews);
            }
            if (previousSeatUseHistory != null) {
                seatUseHistoryRepository.delete(previousSeatUseHistory);
            }

            SeatEntity seatEntity = null;
            if (diaryDtoSeat.id() != null) {
                seatEntity = seatRepository.findById(diaryDtoSeat.id()).orElse(null);
            }

            SeatUseHistoryEntity seatUseHistoryEntity = SeatUseHistoryEntity.builder()
                .diaryEntity(diaryEntity)
                .seatEntity(seatEntity)
                .seatName(diaryDtoSeat.name())
                .build();
            seatUseHistoryRepository.save(seatUseHistoryEntity);
        }

        if (gameRecordEntity != null && !gameRecordEntity.getTeamEntity().getId().equals(teamEntity.getId())) {
            var previousTeam = gameRecordEntity.getTeamEntity();
            var previousResult = gameRecordEntity.getResultType();
            gameRecordEntity.updateRecord(teamEntity, previousTeam,
                    previousResult.equals(MatchEnum.ResultType.WIN) ? MatchEnum.ResultType.LOSS
                            : previousResult.equals(MatchEnum.ResultType.LOSS) ? MatchEnum.ResultType.WIN
                                    : previousResult);
            gameRecordRepository.save(gameRecordEntity);
        }
    }

    @Transactional
    public void deleteDiary(Long diaryId) {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }
        MemberEntity member = memberRepository.findById(Objects.requireNonNull(id))
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var diaryEntity = diaryRepository.findByMemberIdAndId(id, diaryId)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));

        var gameRecordEntity = gameRecordRepository.findByMemberAndDiaryEntityId(member, diaryId);
        if (gameRecordEntity != null) {
            gameRecordRepository.delete(gameRecordEntity);
        }

        fileRefDomainService.deleteFileRefs(RefType.DIARY, diaryId);
        diaryFoodDomainService.deleteFoods(RefType.DIARY, diaryId);
        partnerDomainService.deletePartners(RefType.DIARY, diaryId);

        var previousSeatUseHistory = seatUseHistoryRepository.findByDiaryEntityId(diaryId);
        if (previousSeatUseHistory != null) {
            seatUseHistoryRepository.delete(previousSeatUseHistory);
        }

        var previousSeatReviews = seatReviewRepository.findBySeatUseHistoryEntity(previousSeatUseHistory);
        if (!previousSeatReviews.isEmpty()) {
            seatReviewRepository.deleteAll(previousSeatReviews);
        }

        diaryRepository.delete(diaryEntity);
    }

    private List<CommonDto.PartnerSaveRequest> toPartnerSaveRequests(List<DiaryDomain.PartnerDto> partnerDtoList) {
        if (partnerDtoList == null || partnerDtoList.isEmpty()) {
            return List.of();
        }
        return partnerDtoList.stream().map(dto -> new CommonDto.PartnerSaveRequest(dto.name(), dto.teamId())).toList();
    }

}
