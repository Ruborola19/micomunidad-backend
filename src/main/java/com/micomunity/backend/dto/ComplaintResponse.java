package com.micomunity.backend.dto;

import lombok.Data;

@Data
public class ComplaintResponse {
    private Long id;
    private String content;
    private String creationDate;
    private Long authorId;
    private String authorName;
    private String communityCode;
    private boolean anonymous;
    private String response;
    private String responseDate;
    private String imageUrl;
}
