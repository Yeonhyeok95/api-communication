package com.goldoogi.api_communication.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.goldoogi.api_communication.entity.DCPostEntity;

@Repository
public interface DCPostRepository extends JpaRepository<DCPostEntity, Integer> {
    List<DCPostEntity> findTop1000ByOrderByCreatedAtDesc();
}
