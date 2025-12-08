package com.echommo.repository;

import com.echommo.entity.WeatherText;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WeatherTextRepository extends JpaRepository<WeatherText, Integer> {

    @Query(value = "SELECT content FROM weather_text ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<String> findRandomContent();
}