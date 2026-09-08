package com.example.acres.service;

import com.example.acres.dto.AdminDtos.AuditResponse;
import com.example.acres.entity.AuditLog;
import com.example.acres.entity.User;
import com.example.acres.repository.AuditLogRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(User u, String action, String type, String id, String oldV, String newV, String ip) {
        AuditLog a = new AuditLog();
        a.setUser(u);
        a.setAction(action);
        a.setEntityType(type);
        a.setEntityId(id);
        a.setOldValue(oldV);
        a.setNewValue(newV);
        a.setCreatedAt(Instant.now());
        a.setIpAddress(ip);
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public Page<AuditResponse> page(String action, String search, Instant from, Instant to, Pageable pageable) {
        String actionFilter = action == null || action.isBlank() ? null : action.trim();
        String q = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        final Instant fromFilter = from;
        final Instant toFilter = to;

        Specification<AuditLog> spec = (root, query, cb) -> {
            root.fetch("user", JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (actionFilter != null) {
                predicates.add(cb.equal(root.get("action"), actionFilter));
            }
            if (q != null) {
                String pattern = "%" + q + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("user").get("name"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("entityType"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("entityId"), "")), pattern)));
            }
            if (fromFilter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromFilter));
            }
            if (toFilter != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toFilter));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };

        return repo.findAll(spec, pageable).map(this::toResponse);
    }

    private AuditResponse toResponse(AuditLog a) {
        User u = a.getUser();
        return new AuditResponse(
                a.getId(),
                u == null ? null : u.getId(),
                u == null ? null : u.getName(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getOldValue(),
                a.getNewValue(),
                a.getCreatedAt(),
                a.getIpAddress());
    }
}
