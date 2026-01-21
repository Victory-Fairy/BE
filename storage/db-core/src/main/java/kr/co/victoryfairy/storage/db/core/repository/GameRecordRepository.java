package kr.co.victoryfairy.storage.db.core.repository;

import io.dodn.springboot.core.enums.DiaryEnum;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.storage.db.core.entity.DiaryEntity;
import kr.co.victoryfairy.storage.db.core.entity.GameRecordEntity;
import kr.co.victoryfairy.storage.db.core.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRecordRepository extends JpaRepository<GameRecordEntity, Long> {
    List<GameRecordEntity> findByMemberAndSeason(MemberEntity member, String season);
    List<GameRecordEntity> findByMemberId(Long memberId);
    GameRecordEntity findByMemberAndDiaryEntityId(MemberEntity member, Long diaryId);

    // 리그 타입별 조회 메서드
    List<GameRecordEntity> findByMemberAndSeasonAndLeagueType(
            MemberEntity member,
            String season,
            MatchEnum.LeagueType leagueType
    );

    List<GameRecordEntity> findByMemberAndLeagueType(
            MemberEntity member,
            MatchEnum.LeagueType leagueType
    );
}
