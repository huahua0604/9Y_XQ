package com.hospital.req.repo;

import com.hospital.req.entity.ApprovalComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalCommentRepository extends JpaRepository<ApprovalComment, Long> {}

