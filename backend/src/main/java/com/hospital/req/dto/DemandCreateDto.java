package com.hospital.req.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DemandCreateDto {
    @NotBlank
    private String title;

    @NotBlank
    private String category;
    
    private String impactScope;
    private String relatedSystems;
    private String contactPhone;
    private String location;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    private boolean submit;
}

