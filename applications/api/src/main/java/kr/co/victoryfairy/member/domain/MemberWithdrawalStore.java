package kr.co.victoryfairy.member.domain;

public interface MemberWithdrawalStore {

    boolean memberExists(Long memberId);

    boolean profileExists(Long memberId);

    void delete(Long memberId, WithdrawalReason reason);
}
