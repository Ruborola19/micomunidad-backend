package com.micomunity.backend.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ComplaintDTO {
    private Long id;
    private String content;
    private String creationDate;
    private Long authorId;
    private String authorName;
    private String communityCode;
    private boolean anonymous;
    private String response;
    private String imageUrl;
    private MultipartFile image;

    // DTOs específicos para crear y responder quejas
    @Data
    public static class CreateComplaintRequest {
        private String content;
        private boolean anonymous;
    }

    @Data
    public static class ReplyComplaintRequest {
        private String response;
    }
} 