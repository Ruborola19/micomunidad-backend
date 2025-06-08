package com.micomunity.backend.service;

import com.micomunity.backend.dto.ComplaintResponse;
import com.micomunity.backend.dto.ComplaintsPageResponse;
import com.micomunity.backend.model.Community;
import com.micomunity.backend.model.Complaint;
import com.micomunity.backend.model.Role;
import com.micomunity.backend.model.User;
import com.micomunity.backend.repository.ComplaintRepository;
import com.micomunity.backend.exception.StorageException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final FileStorageService fileStorageService;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Transactional
    public Complaint createComplaint(User user, String content, boolean anonymous, MultipartFile image) {
        validateUserAndCommunity(user);

        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setAnonymous(anonymous);
        complaint.setContent(content);
        complaint.setCommunity(user.getCommunity());
        complaint.setCreationDate(LocalDateTime.now());

        if (image != null && !image.isEmpty()) {
            try {
                String imagePath = fileStorageService.storeFile(image, "quejas");
                complaint.setImagePath(imagePath);
            } catch (StorageException e) {
                throw new RuntimeException("Error storing image: " + e.getMessage());
            }
        }

        Complaint savedComplaint = complaintRepository.save(complaint);
        return savedComplaint;
    }

    @Transactional
    public Complaint replyToComplaint(Long complaintId, String responseText, User user) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (!user.getCommunity().getId().equals(complaint.getCommunity().getId())) {
            throw new RuntimeException("No tienes permiso para responder a esta queja");
        }

        complaint.setResponse(responseText);
        complaint.setResponseDate(LocalDateTime.now());

        return complaintRepository.save(complaint);
    }

    public ComplaintResponse toResponse(Complaint complaint, String baseUrl) {
        String imageUrl = null;
        if (complaint.getImagePath() != null) {
            imageUrl = baseUrl + "/uploads/" + complaint.getImagePath().replace("\\", "/");
        }

        ComplaintResponse response = new ComplaintResponse();
        response.setId(complaint.getId());
        response.setContent(complaint.getContent());
        response.setCreationDate(complaint.getCreationDate().toString());
        response.setAnonymous(complaint.isAnonymous());
        response.setResponse(complaint.getResponse());
        response.setResponseDate(complaint.getResponseDate() != null ? complaint.getResponseDate().toString() : null);
        response.setImageUrl(imageUrl);
        response.setAuthorId(complaint.getUser().getId());
        response.setAuthorName(complaint.isAnonymous() ? "Anónimo" : complaint.getUser().getFullName());
        response.setCommunityCode(complaint.getCommunity().getCommunityCode());
        return response;
    }

    public ComplaintsPageResponse getComplaintsByUser(User user, int page, int size, String baseUrl) {
        Page<Complaint> complaintPage = complaintRepository.findByUser(user, 
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationDate")));
        
        List<ComplaintResponse> responseList = complaintPage.getContent().stream()
                .map(c -> toResponse(c, baseUrl))
                .toList();

        ComplaintsPageResponse response = new ComplaintsPageResponse();
        response.setComplaints(responseList);
        response.setTotalPages(complaintPage.getTotalPages());
        response.setCurrentPage(complaintPage.getNumber());
        return response;
    }

    public ComplaintsPageResponse getComplaintsByCommunity(User user, int page, int size, String baseUrl) {
        Page<Complaint> complaintPage = complaintRepository.findByCommunity(user.getCommunity(), 
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationDate")));
        
        List<ComplaintResponse> responseList = complaintPage.getContent().stream()
                .map(c -> toResponse(c, baseUrl))
                .toList();

        ComplaintsPageResponse response = new ComplaintsPageResponse();
        response.setComplaints(responseList);
        response.setTotalPages(complaintPage.getTotalPages());
        response.setCurrentPage(complaintPage.getNumber());
        return response;
    }

    @Transactional
    public void deleteComplaint(Long complaintId, User user) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        // Verificar si el usuario es el presidente o el autor de la queja
        boolean isPresident = user.getRole() == Role.PRESIDENTE;
        boolean isAuthor = complaint.getUser().getId().equals(user.getId());
        boolean isSameCommunity = user.getCommunity().getId().equals(complaint.getCommunity().getId());

        if (!isSameCommunity) {
            throw new RuntimeException("No tienes permiso para eliminar esta queja");
        }

        if (!isPresident && !isAuthor) {
            throw new RuntimeException("Solo el presidente o el autor pueden eliminar esta queja");
        }

        // Si hay una imagen, intentar eliminarla
        if (complaint.getImagePath() != null) {
            try {
                Path imagePath = Paths.get("uploads").resolve(complaint.getImagePath().replace("/", File.separator));
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                // Log el error pero continuar con el borrado de la queja
                System.err.println("No se pudo eliminar la imagen: " + e.getMessage());
            }
        }

        complaintRepository.delete(complaint);
    }

    private void validateUserAndCommunity(User user) {
        if (user == null) {
            throw new RuntimeException("Usuario no válido");
        }
        if (user.getCommunity() == null) {
            throw new RuntimeException("El usuario no pertenece a ninguna comunidad");
        }
    }
}
