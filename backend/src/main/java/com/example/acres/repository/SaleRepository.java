package com.example.acres.repository;

import com.example.acres.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {
    @Query("select coalesce(sum(s.acres),0) from Sale s")
    BigDecimal sumAcres();

    @Query("""
            select u.id, u.name, coalesce(sum(s.acres), 0)
            from Sale s join s.user u
            group by u.id, u.name
            order by coalesce(sum(s.acres), 0) desc, u.name asc
            """)
    List<Object[]> leaderboard();

    Optional<Sale> findById(Long id);

    List<Sale> findTop10ByOrderBySaleDateDescCreatedAtDesc();

    List<Sale> findByUserIdOrderBySaleDateDescCreatedAtDesc(Long userId);

    Page<Sale> findByUserIdOrderBySaleDateDescCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
