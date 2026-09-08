package com.example.acres.service;

import com.example.acres.dto.SaleDtos.SaleRequest;
import com.example.acres.dto.SaleDtos.SaleResponse;
import com.example.acres.entity.Sale;
import com.example.acres.entity.User;
import com.example.acres.repository.SaleRepository;
import com.example.acres.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    public Page<SaleResponse> mySalesPage(User u, Pageable pageable) {
        return sales.findByUserIdOrderBySaleDateDescCreatedAtDesc(u.getId(), pageable).map(this::dto);
    }

    public long countForUser(User u) {
        return sales.countByUserId(u.getId());
    }

    public java.util.List<SaleResponse> mySales(User u) {
        return sales.findByUserIdOrderBySaleDateDescCreatedAtDesc(u.getId()).stream().map(this::dto).toList();
    }

    public Page<SaleResponse> page(String search, Long userId, LocalDate from, LocalDate to, Pageable pageable) {
        String q = search == null || search.isBlank() ? null : search.trim();
        return sales.search(q, userId, from, to, pageable).map(this::dto);
    }

    public List<SaleResponse> export(String search, Long userId, LocalDate from, LocalDate to) {
        String q = search == null || search.isBlank() ? null : search.trim();
        return sales.exportSearch(q, userId, from, to).stream().map(this::dto).toList();
    }

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

    private String saleSummary(SaleResponse s) {
        return s.salesperson() + " | " + s.acres() + " ac | " + s.saleDate();
    }
}
