package com.micomunity.backend.controller;

import com.micomunity.backend.dto.ComplaintRequest;
import com.micomunity.backend.dto.ComplaintResponse;
import com.micomunity.backend.dto.ComplaintsPageResponse;
import com.micomunity.backend.model.Complaint;
import com.micomunity.backend.model.User;
import com.micomunity.backend.service.ComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintResponse> createComplaint(
            @RequestPart("request") ComplaintRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest
    ) {
        Complaint complaint = complaintService.createComplaint(
            user, 
            request.getContent(), 
            request.isAnonymous(), 
            image
        );
        ComplaintResponse response = complaintService.toResponse(complaint, getBaseUrl(httpRequest));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<ComplaintsPageResponse> getMyComplaints(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        ComplaintsPageResponse response = complaintService.getComplaintsByUser(user, page, size, getBaseUrl(request));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/community")
    public ResponseEntity<ComplaintsPageResponse> getCommunityComplaints(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        ComplaintsPageResponse response = complaintService.getComplaintsByCommunity(user, page, size, getBaseUrl(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<ComplaintResponse> replyToComplaint(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user,
            HttpServletRequest request
    ) {
        String responseText = body.get("response");
        System.out.println("----> ID: " + id);
        System.out.println("----> Response: " + responseText);
        System.out.println("----> User: " + (user != null ? user.getEmail() : "usuario nulo"));

        Complaint complaint = complaintService.replyToComplaint(id, responseText, user);
        ComplaintResponse complaintResponse = complaintService.toResponse(complaint, getBaseUrl(request));
        return ResponseEntity.ok(complaintResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        complaintService.deleteComplaint(id, user);
        return ResponseEntity.ok().build();
    }

    private String getBaseUrl(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        return baseUrl;
    }
}
