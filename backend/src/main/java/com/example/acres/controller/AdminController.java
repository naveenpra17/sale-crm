package com.example.acres.controller;

import com.example.acres.dto.AdminDtos.AuditResponse;
import com.example.acres.dto.AdminDtos.UserResponse;
import com.example.acres.dto.PasswordDtos.ResetPasswordRequest;
import com.example.acres.dto.ProjectDtos.ProjectRequest;
import com.example.acres.dto.ProjectDtos.ProjectResponse;
import com.example.acres.dto.SaleDtos.SaleRequest;
import com.example.acres.dto.SaleDtos.SaleResponse;
import com.example.acres.dto.UserDtos.UserRequest;
import com.example.acres.dto.UserDtos.UserUpdateRequest;
import com.example.acres.entity.User;
import com.example.acres.repository.AuditLogRepository;
import com.example.acres.service.AuditService;
import com.example.acres.service.CurrentUserService;
import com.example.acres.service.ProjectService;
import com.example.acres.service.SaleService;
import com.example.acres.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final int MAX_PAGE_SIZE = 100;

    private final CurrentUserService current;
    private final ProjectService project;
    private final UserService users;
    private final SaleService sales;
    private final AuditLogRepository auditRepo;

    public AdminController(CurrentUserService current, ProjectService project, UserService users, SaleService sales, AuditLogRepository auditRepo) {
        this.current = current;
        this.project = project;
        this.users = users;
        this.sales = sales;
        this.auditRepo = auditRepo;
    }

    @GetMapping("/project")
    public ProjectResponse project() {
        return project.dto(project.get());
    }

    @PutMapping("/project")
    public ProjectResponse updateProject(@Valid @RequestBody ProjectRequest r, HttpServletRequest req) {
        return project.update(r, current.get(), req.getRemoteAddr());
    }

    @GetMapping("/users")
    public Page<UserResponse> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return users.page(search, active, role, capped(pageable)).map(this::userResponse);
    }

    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody UserRequest r, HttpServletRequest req) {
        return userResponse(users.create(r, current.get(), req.getRemoteAddr()));
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest r, HttpServletRequest req) {
        return userResponse(users.update(id, r, current.get(), req.getRemoteAddr()));
    }

    @PostMapping("/users/{id}/reset-password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest r, HttpServletRequest req) {
        users.resetPassword(id, r.newPassword(), current.get(), req.getRemoteAddr());
    }

    @DeleteMapping("/users/{id}")
    public void deactivate(@PathVariable Long id, HttpServletRequest req) {
        users.delete(id, current.get(), req.getRemoteAddr());
    }

    @PostMapping("/users/{id}/reactivate")
    public UserResponse reactivate(@PathVariable Long id, HttpServletRequest req) {
        return userResponse(users.reactivate(id, current.get(), req.getRemoteAddr()));
    }

    @GetMapping("/sales")
    public Page<SaleResponse> sales(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        return sales.page(search, userId, fromDate, toDate, capped(pageable));
    }

    @GetMapping("/sales/export")
    public ResponseEntity<String> exportSales(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        StringBuilder csv = new StringBuilder("Date,Salesperson,Acres,Buyer,Plot,Notes\n");
        sales.export(search, userId, fromDate, toDate).forEach(s -> csv.append(csvLine(s)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @PostMapping("/sales")
    public SaleResponse createSale(@Valid @RequestBody SaleRequest r, HttpServletRequest req) {
        return sales.create(r, current.get(), req.getRemoteAddr());
    }

    @GetMapping("/sales/{id}")
    public SaleResponse sale(@PathVariable Long id) {
        return sales.get(id);
    }

    @PutMapping("/sales/{id}")
    public SaleResponse updateSale(@PathVariable Long id, @Valid @RequestBody SaleRequest r, HttpServletRequest req) {
        return sales.update(id, r, current.get(), req.getRemoteAddr());
    }

    @DeleteMapping("/sales/{id}")
    public void deleteSale(@PathVariable Long id, HttpServletRequest req) {
        sales.delete(id, current.get(), req.getRemoteAddr());
    }

    @GetMapping("/audit-logs")
    public Page<AuditResponse> audit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditRepo.search(action, search, from, to, capped(pageable))
                .map(a -> new AuditResponse(a.getId(), a.getUser() == null ? null : a.getUser().getId(),
                        a.getUser() == null ? null : a.getUser().getName(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getOldValue(), a.getNewValue(), a.getCreatedAt(), a.getIpAddress()));
    }

    private Pageable capped(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }

    private UserResponse userResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.isActive(),
                u.isMustChangePassword(), u.getCreatedAt(), u.getUpdatedAt(), u.getLastLoginAt());
    }

    private String csvLine(SaleResponse s) {
        return String.join(",", quote(s.saleDate().toString()), quote(s.salesperson()), quote(s.acres().toPlainString()),
                quote(s.buyerName()), quote(s.plotReference()), quote(s.notes())) + "\n";
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safe = value;
        if (!safe.isEmpty()) {
            char first = safe.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                safe = "'" + safe;
            }
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
