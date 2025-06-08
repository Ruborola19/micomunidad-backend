package com.micomunity.backend.dto;

import lombok.Data;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String communityCode;
    private String authorName;
    private String authorRole;
    private String creationDate;
} 