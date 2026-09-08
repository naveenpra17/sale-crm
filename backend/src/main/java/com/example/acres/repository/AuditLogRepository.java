package com.example.acres.repository;

import com.example.acres.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            select a from AuditLog a left join a.user u
            where (:action is null or a.action = :action)
              and (:search is null or lower(coalesce(u.name,'')) like lower(concat('%', :search, '%'))
                or lower(coalesce(a.entityType,'')) like lower(concat('%', :search, '%'))
                or lower(coalesce(a.entityId,'')) like lower(concat('%', :search, '%')))
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            """)
    Page<AuditLog> search(@Param("action") String action, @Param("search") String search,
                          @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
