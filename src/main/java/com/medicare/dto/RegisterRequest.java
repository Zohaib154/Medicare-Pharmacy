package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Set;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank @Size(min=3, max=50)
    private String username;

    @NotBlank @Size(min=6)
    private String password;

    @NotBlank
    private String fullName;

    @Email
    private String email;

    private String contactNumber;
    private String licenseNumber;
    private Set<String> roles;
}


