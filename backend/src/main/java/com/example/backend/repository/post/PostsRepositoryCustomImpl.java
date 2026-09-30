package com.example.backend.repository.post;

import com.example.backend.entity.Posts;
import com.example.backend.entity.QPosts;
import com.example.backend.enums.PostStatus;
import com.example.backend.enums.PostVisibility;
import com.example.backend.enums.ReportStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.example.backend.entity.QTags;

import static com.example.backend.entity.QPosts.posts;
import static com.example.backend.entity.QReport.report;

@Repository
@RequiredArgsConstructor
public class PostsRepositoryCustomImpl implements PostsRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Posts> findAllPostsWithDetails(Pageable pageable) {
        BooleanExpression condition = posts.deleted.eq(0)
                .and(posts.status.isNull().or(posts.status.eq(PostStatus.ACTIVE)))
                .and(posts.location.isNull().or(posts.location.deleted.isNull()).or(posts.location.deleted.eq(0)))
                .and(posts.visibility.isNull().or(posts.visibility.ne(PostVisibility.PRIVATE)))
                .and(JPAExpressions.selectOne()
                        .from(report)
                        .where(report.post.eq(posts).and(report.status.eq(ReportStatus.PENDING)))
                        .notExists());

        List<Posts> content = queryFactory
                .selectFrom(posts)
                .leftJoin(posts.user).fetchJoin()
                .leftJoin(posts.location).fetchJoin()
                .where(condition)
                .orderBy(posts.createdDate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(posts.count())
                .from(posts)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Optional<Posts> findByIdWithDetails(String id) {
        BooleanExpression condition = posts.id.eq(id)
                .and(posts.deleted.eq(0))
                .and(posts.location.isNull().or(posts.location.deleted.isNull()).or(posts.location.deleted.eq(0)));

        Posts post = queryFactory
                .selectFrom(posts)
                .leftJoin(posts.user).fetchJoin()
                .leftJoin(posts.location).fetchJoin()
                .where(condition)
                .fetchOne();

        return Optional.ofNullable(post);
    }

    @Override
    public Page<Posts> findAllPostsIncludeDeleted(Pageable pageable) {
        List<Posts> content = queryFactory
                .selectFrom(posts)
                .join(report).on(report.post.eq(posts))
                .leftJoin(posts.user).fetchJoin()
                .leftJoin(posts.location).fetchJoin()
                .groupBy(posts.id)
                .orderBy(report.count().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(posts.countDistinct())
                .from(posts)
                .join(report).on(report.post.eq(posts))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<Posts> searchPosts(String query, Pageable pageable) {
        QTags qTags = QTags.tags;
        
        BooleanExpression condition = posts.deleted.eq(0)
                .and(posts.status.isNull().or(posts.status.eq(PostStatus.ACTIVE)))
                .and(posts.visibility.isNull().or(posts.visibility.ne(PostVisibility.PRIVATE)))
                .and(JPAExpressions.selectOne()
                        .from(report)
                        .where(report.post.eq(posts).and(report.status.eq(ReportStatus.PENDING)))
                        .notExists())
                .and(posts.caption.containsIgnoreCase(query)
                        .or(qTags.name.containsIgnoreCase(query))
                        .or(posts.location.name.containsIgnoreCase(query)));

        List<Posts> content = queryFactory
                .selectFrom(posts).distinct()
                .leftJoin(posts.user).fetchJoin()
                .leftJoin(posts.location).fetchJoin()
                .leftJoin(posts.tags, qTags)
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(posts.countDistinct())
                .from(posts)
                .leftJoin(posts.tags, qTags)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
