package kr.co.victoryfairy.member.infrastructure.persistence;

import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryFoodRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.PartnerRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.member.domain.MemberWithdrawalStore;
import kr.co.victoryfairy.member.domain.WithdrawalReason;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.WithdrawalReasonEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.WithdrawalReasonRepository;
import kr.co.victoryfairy.shared.domain.RefType;
import org.springframework.stereotype.Repository;

@Repository
public class MemberWithdrawalPersistenceAdapter implements MemberWithdrawalStore {

    private final MemberRepository members;
    private final MemberInfoRepository profiles;
    private final GameRecordRepository records;
    private final DiaryRepository diaries;
    private final DiaryFoodRepository foods;
    private final PartnerRepository partners;
    private final SeatUseHistoryRepository seatUses;
    private final SeatReviewRepository seatReviews;
    private final WithdrawalReasonRepository reasons;

    public MemberWithdrawalPersistenceAdapter(MemberRepository members, MemberInfoRepository profiles,
            GameRecordRepository records, DiaryRepository diaries, DiaryFoodRepository foods,
            PartnerRepository partners, SeatUseHistoryRepository seatUses, SeatReviewRepository seatReviews,
            WithdrawalReasonRepository reasons) {
        this.members = members;
        this.profiles = profiles;
        this.records = records;
        this.diaries = diaries;
        this.foods = foods;
        this.partners = partners;
        this.seatUses = seatUses;
        this.seatReviews = seatReviews;
        this.reasons = reasons;
    }

    public boolean memberExists(Long memberId) {
        return members.existsById(memberId);
    }

    public boolean profileExists(Long memberId) {
        return profiles.findByMemberEntity_Id(memberId).isPresent();
    }

    public void delete(Long memberId, WithdrawalReason reason) {
        var member = members.findById(memberId).orElseThrow();
        var profile = profiles.findByMemberEntity(member).orElseThrow();
        var diaryRows = diaries.findByMemberId(memberId);
        var recordRows = records.findByMemberId(memberId);
        var diaryIds = diaryRows.stream().map(row -> row.getId()).toList();
        var foodRows = foods.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds);
        var partnerRows = partners.findByRefTypeAndRefIdIn(RefType.DIARY, diaryIds);
        var seatUseRows = seatUses.findAllByDiaryEntityIdIn(diaryIds);
        var seatReviewRows = seatReviews
            .findAllBySeatUseHistoryEntityIdIn(seatUseRows.stream().map(row -> row.getId()).toList());

        profiles.delete(profile);
        foods.deleteAll(foodRows);
        partners.deleteAll(partnerRows);
        records.deleteAll(recordRows);
        members.delete(member);
        seatReviews.deleteAll(seatReviewRows);
        seatUses.deleteAll(seatUseRows);
        diaries.deleteAll(diaryRows);
        reasons.save(WithdrawalReasonEntity.builder().id(reason.id()).reason(reason.reason()).build());
    }
}
