package com.hospital.req.entity;

public enum RequestStatus {
    DRAFT, SUBMITTED,
    ADMIN_APPROVED, ADMIN_REJECTED,
    REVIEW_APPROVED, REVIEW_REJECTED,
    COMPLETED
}