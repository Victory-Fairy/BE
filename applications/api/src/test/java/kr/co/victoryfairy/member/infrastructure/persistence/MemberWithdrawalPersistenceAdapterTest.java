package kr.co.victoryfairy.member.infrastructure.persistence;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryFoodRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.DiaryRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.GameRecordRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.PartnerRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatReviewRepository;
import kr.co.victoryfairy.diary.infrastructure.persistence.repository.SeatUseHistoryRepository;
import kr.co.victoryfairy.member.domain.WithdrawalReason;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberInfoEntity;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberInfoRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.MemberRepository;
import kr.co.victoryfairy.member.infrastructure.persistence.repository.WithdrawalReasonRepository;
import org.junit.jupiter.api.Test;

class MemberWithdrawalPersistenceAdapterTest {

    @Test
    void preservesRelatedRowDeletionOrder() {
        var members = mock(MemberRepository.class);
        var profiles = mock(MemberInfoRepository.class);
        var records = mock(GameRecordRepository.class);
        var diaries = mock(DiaryRepository.class);
        var foods = mock(DiaryFoodRepository.class);
        var partners = mock(PartnerRepository.class);
        var seatUses = mock(SeatUseHistoryRepository.class);
        var seatReviews = mock(SeatReviewRepository.class);
        var reasons = mock(WithdrawalReasonRepository.class);
        var member = MemberEntity.builder().id(7L).build();
        var profile = MemberInfoEntity.builder().id(8L).memberEntity(member).build();
        when(members.findById(7L)).thenReturn(Optional.of(member));
        when(profiles.findByMemberEntity(member)).thenReturn(Optional.of(profile));
        when(diaries.findByMemberId(7L)).thenReturn(List.of());
        when(records.findByMemberId(7L)).thenReturn(List.of());
        when(foods.findByRefTypeAndRefIdIn(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of());
        when(partners.findByRefTypeAndRefIdIn(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of());
        when(seatUses.findAllByDiaryEntityIdIn(List.of())).thenReturn(List.of());
        when(seatReviews.findAllBySeatUseHistoryEntityIdIn(List.of())).thenReturn(List.of());
        var adapter = new MemberWithdrawalPersistenceAdapter(members, profiles, records, diaries, foods, partners,
                seatUses, seatReviews, reasons);

        adapter.delete(7L, new WithdrawalReason(null, "reason"));

        var order = inOrder(profiles, foods, partners, records, members, seatReviews, seatUses, diaries, reasons);
        order.verify(profiles).delete(profile);
        order.verify(foods).deleteAll(List.of());
        order.verify(partners).deleteAll(List.of());
        order.verify(records).deleteAll(List.of());
        order.verify(members).delete(member);
        order.verify(seatReviews).deleteAll(List.of());
        order.verify(seatUses).deleteAll(List.of());
        order.verify(diaries).deleteAll(List.of());
        order.verify(reasons).save(org.mockito.ArgumentMatchers.argThat(reason -> reason.getReason().equals("reason")));
    }
}
