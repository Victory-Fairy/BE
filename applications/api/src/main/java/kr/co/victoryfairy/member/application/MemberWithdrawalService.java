package kr.co.victoryfairy.member.application;

import kr.co.victoryfairy.shared.domain.RefType;
import kr.co.victoryfairy.member.presentation.MyPageDomain;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.WithdrawalReasonEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryFoodRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.PartnerRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.WithdrawalReasonRepository;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    private final MemberRepository memberRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final GameRecordRepository gameRecordRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryFoodRepository diaryFoodRepository;
    private final PartnerRepository partnerRepository;
    private final SeatUseHistoryRepository seatUseHistoryRepository;
    private final SeatReviewRepository seatReviewRepository;
    private final WithdrawalReasonRepository withdrawalRepository;

    @Transactional
    public void deleteMember(MyPageDomain.DeleteAccountRequest request) {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        var memberEntity = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var memberInfoEntity = memberInfoRepository.findByMemberEntity(memberEntity)
            .orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var diaries = diaryRepository.findByMemberId(id);
        var records = gameRecordRepository.findByMemberId(id);
        var diaryIds = diaries.stream().map(entity -> entity.getId()).toList();
        var foods = diaryFoodRepository.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds);
        var partners = partnerRepository.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds);
        var seatUses = seatUseHistoryRepository.findAllByDiaryEntityIdIn(diaryIds);
        var seatReviews = seatReviewRepository
            .findAllBySeatUseHistoryEntityIdIn(seatUses.stream().map(entity -> entity.getId()).toList());

        memberInfoRepository.delete(memberInfoEntity);
        diaryFoodRepository.deleteAll(foods);
        partnerRepository.deleteAll(partners);
        gameRecordRepository.deleteAll(records);
        memberRepository.delete(memberEntity);
        seatReviewRepository.deleteAll(seatReviews);
        seatUseHistoryRepository.deleteAll(seatUses);
        diaryRepository.deleteAll(diaries);
        withdrawalRepository.save(WithdrawalReasonEntity.builder().reason(request.reason()).build());
    }

}
