package com.hospital.req.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="attachments")
@Getter @Setter
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    private DemandRequest request;

    @ManyToOne(optional=false)
    private User uploadedBy;

    @Column(nullable=false)
    private String originalFilename;

    @Column(nullable=false)
    private String storedFilename;

    @Column(nullable=false)
    private String storagePath;

    private String contentType;
    private long size;
    private String sha256;

    @Column(nullable=false)
    private LocalDateTime uploadedAt;

    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private boolean deleted = false;
}
