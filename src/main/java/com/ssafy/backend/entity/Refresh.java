package com.ssafy.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
public class Refresh {

    @Id
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String refresh;

    @Column(nullable = false)
    private LocalDateTime expiryDate;
}