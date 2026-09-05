package kr.co.victoryfairy.member.infrastructure.persistence.repository;

import kr.co.victoryfairy.member.infrastructure.persistence.entity.WithdrawalReasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalReasonRepository extends JpaRepository<WithdrawalReasonEntity, Long> {

}
