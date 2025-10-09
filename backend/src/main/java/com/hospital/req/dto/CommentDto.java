package com.hospital.req.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDto {
    private String content;
    private LocalDateTime occurredAt;
}
