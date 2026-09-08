package com.example.acres.repository;

import com.example.acres.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
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

    @Query("""
            select s from Sale s join s.user u
            where (:search is null or lower(u.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(s.buyerName,'')) like lower(concat('%', :search, '%'))
                or lower(coalesce(s.plotReference,'')) like lower(concat('%', :search, '%')))
              and (:userId is null or u.id = :userId)
              and (:fromDate is null or s.saleDate >= :fromDate)
              and (:toDate is null or s.saleDate <= :toDate)
            """)
    Page<Sale> search(@Param("search") String search, @Param("userId") Long userId,
                      @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, Pageable pageable);

    @Query("""
            select s from Sale s join s.user u
            where (:search is null or lower(u.name) like lower(concat('%', :search, '%'))
                or lower(coalesce(s.buyerName,'')) like lower(concat('%', :search, '%'))
                or lower(coalesce(s.plotReference,'')) like lower(concat('%', :search, '%')))
              and (:userId is null or u.id = :userId)
              and (:fromDate is null or s.saleDate >= :fromDate)
              and (:toDate is null or s.saleDate <= :toDate)
            order by s.saleDate desc, s.createdAt desc
            """)
    List<Sale> exportSearch(@Param("search") String search, @Param("userId") Long userId,
                            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
