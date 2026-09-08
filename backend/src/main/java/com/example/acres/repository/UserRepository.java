package com.example.acres.repository;

import com.example.acres.entity.Role;
import com.example.acres.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:search is null or lower(u.name) like lower(concat('%', :search, '%')) or lower(u.email) like lower(concat('%', :search, '%')))
              and (:active is null or u.active = :active)
              and (:role is null or u.role = :role)
            """)
    Page<User> search(@Param("search") String search, @Param("active") Boolean active, @Param("role") Role role, Pageable pageable);

    long countByRoleAndActive(Role role, boolean active);
}
