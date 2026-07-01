package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Set;

@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}

// ---- in the same package ----
// LoginResponse.java


