package kr.co.victoryfairy.diary.infrastructure.persistence.admin;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import kr.co.victoryfairy.diary.domain.DiaryModel;
import kr.co.victoryfairy.diary.application.admin.AdminDiaryQueryStore;
import kr.co.victoryfairy.diary.infrastructure.persistence.entity.DiaryEntity;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.shared.domain.PageResult;
import kr.co.victoryfairy.shared.infrastructure.persistence.PageUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import static kr.co.victoryfairy.diary.infrastructure.persistence.entity.QDiaryEntity.diaryEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QGameMatchEntity.gameMatchEntity;
import static kr.co.victoryfairy.game.infrastructure.persistence.entity.QTeamEntity.teamEntity;
import static kr.co.victoryfairy.member.infrastructure.persistence.entity.QMemberEntity.memberEntity;
import static kr.co.victoryfairy.member.infrastructure.persistence.entity.QMemberInfoEntity.memberInfoEntity;

@Repository
public class AdminDiaryQueryPersistenceAdapter extends QuerydslRepositorySupport implements AdminDiaryQueryStore {
    private final JPAQueryFactory queries;

    public AdminDiaryQueryPersistenceAdapter(JPAQueryFactory queries) {
        super(DiaryEntity.class);
        this.queries = queries;
    }

    @Override
    public PageResult<DiaryModel.DiaryListResponse> findAll(DiaryModel.DiaryListRequest request) {
        var query = queries.select(Projections.fields(DiaryModel.DiaryListResponse.class, diaryEntity.id,
                        teamEntity.id.as("teamId"), teamEntity.name.as("teamName"), diaryEntity.content,
                        memberEntity.id.as("memberId"), memberInfoEntity.nickNm, gameMatchEntity.matchAt,
                        gameMatchEntity.status, diaryEntity.moodType, diaryEntity.viewType, diaryEntity.weatherType))
                .from(diaryEntity)
                .innerJoin(memberEntity).on(diaryEntity.memberId.eq(memberEntity.id))
                .innerJoin(memberInfoEntity).on(memberEntity.id.eq(memberInfoEntity.memberEntity.id))
                .leftJoin(teamEntity).on(diaryEntity.teamEntity.id.eq(teamEntity.id))
                .leftJoin(gameMatchEntity).on(gameMatchEntity.id.eq(diaryEntity.gameMatchEntity.id))
                .orderBy(diaryEntity.id.desc())
                .where(eqMatchAt(request.date()), eqStatus(request.status()));
        return PageUtils.getPageResult(query, PageRequest.of(request.page() - 1, request.size()));
    }

    private BooleanExpression eqMatchAt(LocalDate matchAt) {
        return matchAt == null ? null : gameMatchEntity.matchAt.goe(matchAt.atStartOfDay())
                .and(gameMatchEntity.matchAt.lt(matchAt.plusDays(1).atStartOfDay()));
    }

    private BooleanExpression eqStatus(MatchEnum.MatchStatus status) {
        return status == null ? null : gameMatchEntity.status.eq(status);
    }
}
