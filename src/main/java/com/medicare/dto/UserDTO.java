package com.medicare.dto;

import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String contactNumber;
    private String licenseNumber;
    private Set<String> roles;
    private Boolean isActive;
    /** Plain-text password for create/update only; never returned in responses. */
    private String password;
    /** Alias accepted from the frontend staff form. */
    private String passwordHash;
}
