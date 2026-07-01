package com.medicare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 15)
    private String contactNumber;

    @Column(length = 50)
    private String licenseNumber;       // Pharmacist licence

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    private String refreshToken;

    public enum Role {
        ROLE_ADMIN,
        ROLE_PHARMACIST,
        ROLE_CASHIER,
        ROLE_STORE_MANAGER,
        ROLE_VIEWER
    }
}


