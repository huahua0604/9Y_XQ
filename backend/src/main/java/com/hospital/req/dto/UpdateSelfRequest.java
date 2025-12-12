package com.hospital.req.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSelfRequest(
        @NotBlank @Size(min=1, max=32) String employeeId,
        @NotBlank @Size(min=1, max=50) String name,
        @NotBlank @Size(min=1, max=50) String department,
        @Size(max=32) String phone
) {}
