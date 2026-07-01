package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientDTO {
    private Long patientId;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Integer age;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER")
    private String gender;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Invalid contact number")
    private String contactNumber;

    @Email
    private String email;

    private String address;
    private String bloodGroup;
    private String allergies;
    private String chronicConditions;
    private String currentMedications;
    private String cnicNumber;
    private String insuranceProvider;
    private String insurancePolicyNo;
    private Boolean isActive;
    private LocalDateTime createdAt;
}


