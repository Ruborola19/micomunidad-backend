package com.micomunity.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintsPageResponse {
    private List<ComplaintResponse> complaints;
    private int totalPages;
    private int currentPage;
}
