package com.hospital.req.dto;

import com.hospital.req.entity.RequestStatus;
import com.hospital.req.entity.Priority;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DemandDetailDto {
    private Long id;
    private String title;
    private String category;
    private Priority priority;
    private LocalDate desiredDueDate;
    private BigDecimal budgetEstimate;
    private String impactScope;
    private String relatedSystems;
    private String contactPhone;
    private String location;
    private String description;

    private String createdByEmpId;
    private String createdByName;
    private String createdByDept;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;

    private RequestStatus status;
    private LocalDateTime adminReviewedAt;
    private LocalDateTime reviewerReviewedAt;
    private String adminOpinion;
    private String reviewerOpinion;

    private List<AttachmentDto> attachments;
    private List<HistoryDto> history;
    private List<CommentViewDto> comments;

    @Data public static class AttachmentDto {
        private Long id;
        private String originalFilename;
        private String contentType;
        private long size;
        private LocalDateTime uploadedAt;
        private LocalDateTime occurredAt;
        private String downloadUrl;
        private String uploadedByEmpId;
        private String uploadedByName;
    }
    @Data public static class HistoryDto {
        private String action;
        private String actorEmpId;
        private String actorName;
        private LocalDateTime occurredAt;
        private String note;
        private Long commentId;
        private Long attachmentId;

    }
    @Data public static class CommentViewDto {
        private Long id;
        private String authorEmpId;
        private String authorName;
        private String authorRole;
        private String content;
        private LocalDateTime createdAt;
    }
}
