package com.hospital.req.entity;

import jakarta.persistence.*;
import lombok.Getter; 
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="request_history")
@Getter @Setter
public class RequestHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) private DemandRequest request;
    @ManyToOne(optional=false) private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 64, nullable = false)
    private HistoryAction action;

    private LocalDateTime occurredAt;

    @Column(length=2000)
    private String note;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "comment_id",
        foreignKey = @ForeignKey(name = "FK_request_history_comment"))
    private ApprovalComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;
    
    @Column(name = "comment_ref_id")
    private Long commentRefId;

    @Column(name = "attachment_ref_id")
    private Long attachmentRefId;
    
    @PrePersist
    public void prePersist(){
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }
}
