package com.ssafy.backend.repository;

import com.ssafy.backend.entity.Refresh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshRepository extends JpaRepository<Refresh, Long> {
    Optional<Refresh> findByRefresh(String refresh);
}