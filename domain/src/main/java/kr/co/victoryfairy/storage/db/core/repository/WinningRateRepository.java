package kr.co.victoryfairy.storage.db.core.repository;

import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import kr.co.victoryfairy.storage.db.core.entity.WinningRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WinningRateRepository extends JpaRepository<WinningRateEntity, Long> {

    Optional<WinningRateEntity> findByMemberAndSeason(MemberEntity member, String season);

}
