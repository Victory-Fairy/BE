package kr.co.victoryfairy.storage.db.core.utils;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import kr.co.victoryfairy.support.model.PageResult;
import org.springframework.data.domain.Pageable;

/**
 * QueryDSL 관련 Paging
 */
public class PageUtils {

    public static <T> PageResult<T> getPageResult(JPAQuery<T> listQuery, Pageable pageable) {
        var totalCount = listQuery.clone()
                .select(Expressions.ONE.count())
                .fetchOne();

        var resultList = listQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageResult<>(resultList, totalCount);
    }
}