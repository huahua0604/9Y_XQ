package com.hospital.req.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name="demand_requests")
@Getter @Setter
public class DemandRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String title;

    @Column(length=40)
    private String category;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    private LocalDate desiredDueDate;

    @Column(precision=12, scale=2)
    private BigDecimal budgetEstimate;

    @Column(length=40)
    private String impactScope; // 个人/本科室/跨科室/全院

    @Column(length=200)
    private String relatedSystems;

    @Column(length=20)
    private String contactPhone;

    @Column(length=100)
    private String location;

    @Lob
    private String description;

    @ManyToOne(optional=false)
    private User createdBy;

    @Column(length=64)
    private String createdByDept;

    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private RequestStatus status = RequestStatus.DRAFT;

    /** 审批时间戳 */
    private LocalDateTime adminReviewedAt;
    private LocalDateTime reviewerReviewedAt;

    /** 审批意见（最后一次） */
    @Column(length=2000)
    private String adminOpinion;

    @Column(length=2000)
    private String reviewerOpinion;

    @OneToMany(mappedBy="request", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<Attachment> attachments;

    @OneToMany(mappedBy="request", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<ApprovalComment> comments;

    @OneToMany(mappedBy="request", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<RequestHistory> history;
}

