package com.ssafy.backend.repository;

import com.ssafy.backend.entity.UserCreatedProblem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreatedProblemRepository extends JpaRepository<UserCreatedProblem, Long> {
}
