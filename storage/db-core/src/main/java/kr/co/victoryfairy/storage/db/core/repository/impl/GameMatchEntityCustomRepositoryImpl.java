package kr.co.victoryfairy.storage.db.core.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchEntityCustomRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static kr.co.victoryfairy.storage.db.core.entity.QGameMatchEntity.gameMatchEntity;

@Repository
public class GameMatchEntityCustomRepositoryImpl extends QuerydslRepositorySupport implements GameMatchEntityCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public GameMatchEntityCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        super(GameMatchEntity.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<GameMatchEntity> findByMatchAt(LocalDate matchAt) {
        return jpaQueryFactory
                .select(Projections.fields(GameMatchEntity.class
                        , gameMatchEntity.id
                        , gameMatchEntity.type
                        , gameMatchEntity.series
                        , gameMatchEntity.season
                        , gameMatchEntity.matchAt
                        , gameMatchEntity.awayTeamEntity
                        , gameMatchEntity.awayNm
                        , gameMatchEntity.awayScore
                        , gameMatchEntity.homeTeamEntity
                        , gameMatchEntity.homeNm
                        , gameMatchEntity.homeScore
                        , gameMatchEntity.stadium
                        , gameMatchEntity.status
                        , gameMatchEntity.reason
                        , gameMatchEntity.isMatchInfoCraw
                ))
                .from(gameMatchEntity)
                .where(this.eqMatchAt(matchAt))
                .fetch();
    }

    private BooleanExpression eqMatchAt(LocalDate matchAt) {
        if (matchAt == null) {
            return null;
        }

        String matchAtStr = matchAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        StringTemplate dbDate = Expressions.stringTemplate(
                "DATE_FORMAT({0}, '%Y-%m-%d')", gameMatchEntity.matchAt);

        return dbDate.eq(matchAtStr);
    }
}
