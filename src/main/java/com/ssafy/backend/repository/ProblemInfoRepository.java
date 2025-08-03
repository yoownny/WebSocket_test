package com.ssafy.backend.repository;

import com.ssafy.backend.entity.ProblemInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemInfoRepository extends JpaRepository<ProblemInfo, Long> {
}
