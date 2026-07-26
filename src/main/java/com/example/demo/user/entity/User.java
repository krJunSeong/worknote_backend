package com.example.demo.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Login ID
    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    // secret password
    @Column(nullable = false)
    private String password;

    // Display name on screen
    @Column(nullable = false, length = 30)
    private String nickname;

    // Create Data
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}