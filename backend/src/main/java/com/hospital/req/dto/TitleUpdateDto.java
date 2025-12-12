package com.hospital.req.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TitleUpdateDto(
        @NotBlank
        @Size(max = 120)
        String title
) {}
