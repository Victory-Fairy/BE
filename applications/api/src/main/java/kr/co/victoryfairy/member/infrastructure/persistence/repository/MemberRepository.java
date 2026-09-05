package kr.co.victoryfairy.member.infrastructure.persistence.repository;

import kr.co.victoryfairy.member.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

}
