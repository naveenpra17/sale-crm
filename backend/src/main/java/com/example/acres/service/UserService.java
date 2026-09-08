package com.example.acres.service;

import com.example.acres.dto.UserDtos.UserRequest;
import com.example.acres.dto.UserDtos.UserUpdateRequest;
import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import com.example.acres.exception.ConflictException;
import com.example.acres.exception.ForbiddenException;
import com.example.acres.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthService auth;
    private final AuditService audit;

    public UserService(UserRepository repo, PasswordEncoder encoder, AuthService auth, AuditService audit) {
        this.repo = repo;
        this.encoder = encoder;
        this.auth = auth;
        this.audit = audit;
    }

    public Page<User> page(String search, Boolean active, String role, Pageable pageable) {
        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            roleEnum = com.example.acres.util.RoleParser.parse(role);
        }
        String q = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        final Boolean activeFilter = active;
        final Role roleFilter = roleEnum;

        Specification<User> spec = Specification.where(null);
        if (q != null) {
            String pattern = "%" + q + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
        }
        if (activeFilter != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), activeFilter));
        }
        if (roleFilter != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), roleFilter));
        }
        return repo.findAll(spec, pageable);
    }

    @Transactional
    public void changePassword(User u, String current, String next, String ip) {
        if (!encoder.matches(current, u.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        u.setPasswordHash(encoder.encode(next));
        u.setMustChangePassword(false);
        bumpTokenVersion(u);
        repo.save(u);
        auth.revokeUserSessions(u.getId());
        audit.log(u, "CHANGE_PASSWORD", "USER", u.getId().toString(), null, "Password changed", ip);
    }

    @Transactional
    public void resetPassword(Long id, String next, User actor, String ip) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setPasswordHash(encoder.encode(next));
        u.setMustChangePassword(true);
        bumpTokenVersion(u);
        repo.save(u);
        auth.revokeUserSessions(id);
        audit.log(actor, "RESET_PASSWORD", "USER", id.toString(), null, "Password reset", ip);
    }

    @Transactional
    public User create(UserRequest r, User actor, String ip) {
        if (repo.findByEmailIgnoreCase(r.email()).isPresent()) {
            throw new ConflictException("Email already exists");
        }
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(com.example.acres.util.RoleParser.parse(r.role()));
        u.setMustChangePassword(r.mustChangePassword());
        u = repo.save(u);
        audit.log(actor, "CREATE_USER", "USER", u.getId().toString(), null, u.getName(), ip);
        return u;
    }

    @Transactional
    public User update(Long id, UserUpdateRequest r, User actor, String ip) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        String normalized = r.email().trim().toLowerCase();
        repo.findByEmailIgnoreCase(normalized).filter(existing -> !existing.getId().equals(id))
                .ifPresent(x -> {
                    throw new ConflictException("Email already exists");
                });

        if (u.getRole() == Role.ADMIN && u.isActive() && (!r.active() || r.role() != Role.ADMIN.name())) {
            ensureAnotherActiveAdmin(id);
        }

        String old = u.getName() + " / " + u.getEmail() + " / " + u.getRole() + " / " + u.isActive();
        u.setName(r.name());
        u.setEmail(normalized);
        u.setRole(com.example.acres.util.RoleParser.parse(r.role()));
        u.setMustChangePassword(r.mustChangePassword());
        if (u.isActive() != r.active()) {
            u.setActive(r.active());
            if (!r.active()) {
                bumpTokenVersion(u);
                auth.revokeUserSessions(u.getId());
            }
            audit.log(actor, r.active() ? "REACTIVATE_USER" : "DEACTIVATE_USER", "USER", id.toString(), old, u.getName(), ip);
        }
        repo.save(u);
        audit.log(actor, "UPDATE_USER", "USER", id.toString(), old, u.getName(), ip);
        return u;
    }

    @Transactional
    public void delete(Long id, User actor, String ip) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (u.getRole() == Role.ADMIN && u.isActive()) {
            ensureAnotherActiveAdmin(id);
        }
        u.setActive(false);
        bumpTokenVersion(u);
        repo.save(u);
        auth.revokeUserSessions(id);
        audit.log(actor, "DEACTIVATE_USER", "USER", id.toString(), null, u.getName(), ip);
    }

    @Transactional
    public User reactivate(Long id, User actor, String ip) {
        User u = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setActive(true);
        repo.save(u);
        audit.log(actor, "REACTIVATE_USER", "USER", id.toString(), null, u.getName(), ip);
        return u;
    }

    private void bumpTokenVersion(User u) {
        u.setTokenVersion(u.getTokenVersion() + 1);
    }

    private void ensureAnotherActiveAdmin(Long excludeId) {
        long activeAdmins = repo.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.isActive() && !u.getId().equals(excludeId))
                .count();
        if (activeAdmins == 0) {
            throw new ForbiddenException("At least one active administrator is required");
        }
    }
}
