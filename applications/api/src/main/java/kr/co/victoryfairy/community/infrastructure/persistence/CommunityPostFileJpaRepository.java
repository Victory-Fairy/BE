package kr.co.victoryfairy.community.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CommunityPostFileJpaRepository extends JpaRepository<CommunityPostFileJpaEntity, Long> {

    List<CommunityPostFileJpaEntity> findByPostIdInOrderByIdAsc(List<Long> postIds);

}
