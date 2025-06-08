package com.micomunity.backend.dto;

import lombok.Data;

@Data
public class ComplaintRequest {
    private String content;
    private boolean anonymous;
}
