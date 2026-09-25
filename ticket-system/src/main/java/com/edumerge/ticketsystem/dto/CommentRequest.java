package com.edumerge.ticketsystem.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private Long actorId;
    private String message;
}
