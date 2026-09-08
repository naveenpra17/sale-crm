package com.example.acres.service;

import com.example.acres.dto.SaleDtos.SaleRequest;
import com.example.acres.dto.SaleDtos.SaleResponse;
import com.example.acres.entity.Sale;
import com.example.acres.entity.User;
import com.example.acres.repository.SaleRepository;
import com.example.acres.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SaleService {
    private final SaleRepository sales;
    private final UserRepository users;
    private final AuditService audit;

    public SaleService(SaleRepository sales, UserRepository users, AuditService audit) {
        this.sales = sales;
        this.users = users;
        this.audit = audit;
    }

    public SaleResponse dto(Sale s) {
        return new SaleResponse(s.getId(), s.getUser().getId(), s.getUser().getName(), s.getAcres(), s.getSaleDate(),
                s.getBuyerName(), s.getPlotReference(), s.getNotes(), s.getCreatedAt(), s.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> mySalesPage(User u, Pageable pageable) {
        return sales.findByUserIdOrderBySaleDateDescCreatedAtDesc(u.getId(), pageable).map(this::dto);
    }

    public long countForUser(User u) {
        return sales.countByUserId(u.getId());
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> mySales(User u) {
        return sales.findByUserIdOrderBySaleDateDescCreatedAtDesc(u.getId()).stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> page(String search, Long userId, LocalDate from, LocalDate to, Pageable pageable) {
        return sales.findAll(saleSearchSpec(search, userId, from, to), pageable).map(this::dto);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> export(String search, Long userId, LocalDate from, LocalDate to) {
        Sort sort = Sort.by(Sort.Direction.DESC, "saleDate", "createdAt");
        return sales.findAll(saleSearchSpec(search, userId, from, to), sort).stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> recentSales(int limit) {
        Pageable page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "saleDate", "createdAt"));
        Specification<Sale> spec = (root, query, cb) -> {
            root.fetch("user", JoinType.INNER);
            if (query != null) {
                query.distinct(true);
            }
            return cb.conjunction();
        };
        return sales.findAll(spec, page).map(this::dto).getContent();
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id) {
        return sales.findById(id).map(this::dto).orElseThrow(() -> new IllegalArgumentException("Sale not found"));
    }

    @Transactional
    public SaleResponse create(SaleRequest r, User actor, String ip) {
        User u = users.findById(r.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!u.isActive()) {
            throw new IllegalArgumentException("Inactive users cannot receive new sales");
        }
        Sale s = new Sale();
        s.setUser(u);
        s.setAcres(r.acres());
        s.setSaleDate(r.saleDate());
        s.setBuyerName(r.buyerName());
        s.setPlotReference(r.plotReference());
        s.setNotes(r.notes());
        s.setCreatedBy(actor);
        s.setUpdatedBy(actor);
        s = sales.save(s);
        audit.log(actor, "CREATE_SALE", "SALE", s.getId().toString(), null, saleSummary(dto(s)), ip);
        return dto(s);
    }

    @Transactional
    public SaleResponse update(Long id, SaleRequest r, User actor, String ip) {
        Sale s = sales.findById(id).orElseThrow(() -> new IllegalArgumentException("Sale not found"));
        String old = saleSummary(dto(s));
        User u = users.findById(r.userId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!u.isActive()) {
            throw new IllegalArgumentException("Inactive users cannot receive new sales");
        }
        s.setUser(u);
        s.setAcres(r.acres());
        s.setSaleDate(r.saleDate());
        s.setBuyerName(r.buyerName());
        s.setPlotReference(r.plotReference());
        s.setNotes(r.notes());
        s.setUpdatedBy(actor);
        s = sales.save(s);
        audit.log(actor, "UPDATE_SALE", "SALE", id.toString(), old, saleSummary(dto(s)), ip);
        return dto(s);
    }

    @Transactional
    public void delete(Long id, User actor, String ip) {
        Sale s = sales.findById(id).orElseThrow(() -> new IllegalArgumentException("Sale not found"));
        audit.log(actor, "DELETE_SALE", "SALE", id.toString(), saleSummary(dto(s)), null, ip);
        sales.delete(s);
    }

    private Specification<Sale> saleSearchSpec(String search, Long userId, LocalDate from, LocalDate to) {
        String q = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        final Long userFilter = userId;
        final LocalDate fromFilter = from;
        final LocalDate toFilter = to;

        return (root, query, cb) -> {
            root.fetch("user", JoinType.INNER);
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (q != null) {
                String pattern = "%" + q + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("user").get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("buyerName"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("plotReference"), "")), pattern)));
            }
            if (userFilter != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userFilter));
            }
            if (fromFilter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("saleDate"), fromFilter));
            }
            if (toFilter != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("saleDate"), toFilter));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String saleSummary(SaleResponse s) {
        return s.salesperson() + " | " + s.acres() + " ac | " + s.saleDate();
    }
}
