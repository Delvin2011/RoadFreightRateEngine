package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.BorderPost;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorderPostRepository extends JpaRepository<BorderPost, UUID> {

    Optional<BorderPost> findByCode(String code);
}
