package com.example.backend.repository.post.report;

import com.example.backend.entity.Posts;
import com.example.backend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReportRepository extends JpaRepository<Report, String>, ReportRepositoryCustom {
    long countByStatus(ReportStatus status);

    boolean existsByPostAndReporterIsNullAndStatus(Posts post, ReportStatus status);

    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.post.id = :postId AND r.status = :status")
    boolean existsByPostIdAndStatus(@Param("postId") String postId, @Param("status") ReportStatus status);
}
