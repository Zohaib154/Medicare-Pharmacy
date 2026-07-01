package com.medicare.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PrescriptionDTO {
    private Long prescriptionId;
    private String rxNumber;

    @NotNull(message = "Patient ID is required")
    private Long patientId;
    private String patientName;

    @NotBlank(message = "Doctor name is required")
    private String doctorName;
    private String doctorLicenseNo;
    private String hospitalClinic;
    private LocalDate issueDate;
    private LocalDate dispensedDate;
    private String diagnosis;
    private String status;
    private String notes;
    private BigDecimal totalAmount;
    private BigDecimal discountPercent;
    private String dispensedByName;

    @NotEmpty(message = "At least one item is required")
    private List<PrescriptionItemDTO> items;
}


