package com.echommo.repository;

import com.echommo.entity.GiftCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GiftCodeRepository extends JpaRepository<GiftCode, String> {
    Optional<GiftCode> findByCode(String code);
}