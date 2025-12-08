package com.echommo.repository;

import com.echommo.entity.FlavorText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlavorTextRepository extends JpaRepository<FlavorText, Integer> {

    // Lấy ngẫu nhiên 1 dòng
    @Query(value = "SELECT content FROM flavor_text ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<String> findRandomContent();
}