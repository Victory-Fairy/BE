package kr.co.victoryfairy.member.application;

import kr.co.victoryfairy.member.presentation.MyPageDomain;
import kr.co.victoryfairy.member.domain.MemberWithdrawalStore;
import kr.co.victoryfairy.member.domain.WithdrawalReason;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    private final MemberWithdrawalStore withdrawalStore;

    @Transactional
    public void deleteMember(MyPageDomain.DeleteAccountRequest request) {
        var id = CurrentRequest.getId();
        if (id == null) {
            throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        }

        if (!withdrawalStore.memberExists(id) || !withdrawalStore.profileExists(id))
            throw new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
        withdrawalStore.delete(id, new WithdrawalReason(null, request.reason()));
    }

}
