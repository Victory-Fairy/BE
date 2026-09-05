package kr.co.victoryfairy.diary.infrastructure.persistence.repository;

import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.WinningRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WinningRateRepository extends JpaRepository<WinningRateEntity, Long> {

    Optional<WinningRateEntity> findByMemberAndSeason(MemberEntity member, String season);

}
