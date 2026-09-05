package kr.co.victoryfairy.diary.application;

import java.util.List;
import kr.co.victoryfairy.diary.domain.Diary;
import kr.co.victoryfairy.diary.domain.DiaryStore;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.diary.domain.SeatUseStore;
import kr.co.victoryfairy.diary.presentation.DiaryDomain;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.TeamReader;
import kr.co.victoryfairy.media.application.FileReferenceService;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import kr.co.victoryfairy.redis.lock.DistributedLock;
import kr.co.victoryfairy.redis.lock.LockName;
import kr.co.victoryfairy.diary.application.PartnerDto;
import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryCommandService {
    private final DiaryStore diaries;
    private final SeatUseStore seatUses;
    private final GameMatchRepository matches;
    private final GameRecordStore records;
    private final MemberStore members;
    private final TeamReader teams;
    private final FileReferenceService files;
    private final DiaryFoodDomainService foods;
    private final PartnerDomainService partners;
    private final GameRecordDomainService gameRecords;

    @Transactional
    @DistributedLock(value = LockName.DIARY_WRITE, key = "#memberId + '_' + #request.gameMatchId()")
    public DiaryDomain.WriteResponse writeDiary(Long memberId, DiaryDomain.WriteRequest request) {
        if (members.findMember(memberId).isEmpty()) throw notFound();
        var match = matches.findDiaryWriteById(request.gameMatchId()).orElseThrow(DiaryCommandService::notFound);
        var team = teams.findById(request.teamId()).orElseThrow(DiaryCommandService::notFound);
        if (diaries.findByMemberAndMatch(memberId, match.id()).isPresent())
            throw new CustomException(HttpStatus.CONFLICT, MessageEnum.Data.FAIL_DUPLICATE);

        var diary = diaries.save(new Diary(null, memberId, match.id(), team.id(), team.name(), request.viewType(),
                request.weather(), request.mood(), request.content(), false, null, null));
        files.saveFileRefs(RefType.DIARY, diary.id(), request.fileId());
        foods.saveFoods(RefType.DIARY, diary.id(), request.foodNameList());
        partners.savePartners(RefType.DIARY, diary.id(), toPartners(request.partnerList()));
        if (request.seat() != null) seatUses.save(diary.id(), request.seat().id(), request.seat().name());
        gameRecords.record(diary);
        return new DiaryDomain.WriteResponse(diary.id());
    }

    @Transactional
    public void updateDiary(Long diaryId, DiaryDomain.UpdateRequest request) {
        Long memberId = requireCurrentMember();
        var team = teams.findById(request.teamId()).orElseThrow(DiaryCommandService::notFound);
        var diary = diaries.findByMemberAndId(memberId, diaryId).orElseThrow(DiaryCommandService::notFound);
        var record = records.findByDiaryId(diaryId);
        diaries.save(diary.update(team.id(), team.name(), request.viewType(), request.mood(), request.weather(),
                request.content()));
        files.replaceFileRefs(RefType.DIARY, diaryId, request.fileId());
        foods.replaceFoods(RefType.DIARY, diaryId, request.foodNameList());
        partners.replacePartners(RefType.DIARY, diaryId, toPartners(request.partnerList()));
        if (request.seat() != null) seatUses.replace(diaryId, request.seat().id(), request.seat().name());
        record.filter(value -> !value.teamId().equals(team.id())).ifPresent(value -> records.save(value.switchTeam(team.id(), team.name())));
    }

    @Transactional
    public void deleteDiary(Long diaryId) {
        Long memberId = requireCurrentMember();
        if (diaries.findByMemberAndId(memberId, diaryId).isEmpty()) throw notFound();
        records.findByDiaryId(diaryId).ifPresent(value -> records.delete(value.id()));
        files.deleteFileRefs(RefType.DIARY, diaryId);
        foods.deleteFoods(RefType.DIARY, diaryId);
        partners.deletePartners(RefType.DIARY, diaryId);
        seatUses.delete(diaryId);
        diaries.delete(diaryId);
    }

    private Long requireCurrentMember() {
        Long id = CurrentRequest.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        if (members.findMember(id).isEmpty()) throw notFound();
        return id;
    }

    private static List<PartnerDto.PartnerSaveRequest> toPartners(List<DiaryDomain.PartnerDto> values) {
        return values == null ? List.of() : values.stream().map(value -> new PartnerDto.PartnerSaveRequest(value.name(), value.teamId())).toList();
    }

    private static CustomException notFound() { return new CustomException(MessageEnum.Data.FAIL_NO_RESULT); }
}
