package com.hospital.req.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hospital.req.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DemandCreateDto {
    @NotBlank
    private String title;
    
    private String category;
    private Priority priority;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate desiredDueDate;

    private BigDecimal budgetEstimate;
    private String impactScope;
    private String relatedSystems;
    private String contactPhone;
    private String location;
    private String description;
    private boolean submit; // true=立即提交；false=保存草稿
}

