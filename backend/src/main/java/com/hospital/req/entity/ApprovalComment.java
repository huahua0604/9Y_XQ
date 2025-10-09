package com.hospital.req.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name="approval_comments")
@Getter @Setter
public class ApprovalComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) private DemandRequest request;
    @ManyToOne(optional=false) private User author;

    @Enumerated(EnumType.STRING)
    private Role authorRole;

    @Column(nullable=false, length=2000)
    private String content;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean deleted = false; 
}
