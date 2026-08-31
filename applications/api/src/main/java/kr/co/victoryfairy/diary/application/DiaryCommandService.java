package kr.co.victoryfairy.diary.application;

import io.dodn.springboot.core.enums.MatchEnum;
import io.dodn.springboot.core.enums.RefType;
import kr.co.victoryfairy.common.model.CommonDto;
import kr.co.victoryfairy.common.service.DiaryFoodDomainService;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.common.service.GameRecordDomainService;
import kr.co.victoryfairy.common.service.PartnerDomainService;
import kr.co.victoryfairy.diary.presentation.DiaryDomain;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.SeatEntity;
import kr.co.victoryfairy.storage.db.core.entity.SeatUseHistoryEntity;
import kr.co.victoryfairy.storage.db.core.repository.DiaryRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchRepository;
import kr.co.victoryfairy.storage.db.core.repository.GameRecordRepository;
import kr.co.victoryfairy.storage.db.core.repository.MemberRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatReviewRepository;
import kr.co.victoryfairy.storage.db.core.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.storage.db.core.repository.TeamRepository;
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

        GameMatchEntity gameMatchEntity = gameMatchRepository.findById(diaryDto.gameMatchId())
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
