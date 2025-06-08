package com.micomunity.backend.controller;

import com.micomunity.backend.dto.*;
import com.micomunity.backend.exception.*;
import com.micomunity.backend.model.Incidencia;
import com.micomunity.backend.model.EstadoIncidencia;
import com.micomunity.backend.model.User;
import com.micomunity.backend.service.IncidenciaService;
import com.micomunity.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
@Validated
public class IncidenciaController {

    private final IncidenciaService incidenciaService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PRESIDENTE', 'VECINO')")
    public ResponseEntity<IncidenciaResponse> crearIncidencia(
            @RequestPart("incidencia") @Valid IncidenciaDTO incidenciaDTO,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen,
            @AuthenticationPrincipal User user
    ) {
        Incidencia incidencia = incidenciaService.crearIncidencia(user, incidenciaDTO, imagen);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return ResponseEntity.ok(incidenciaService.convertirADTO(incidencia, baseUrl));
    }

    @GetMapping("/community")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PRESIDENTE', 'VECINO')")
    public ResponseEntity<IncidenciasPaginadas> obtenerIncidenciasComunidad(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaCreacion") String sortField,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        // Mapear los campos del frontend a los campos de la entidad
        String dbSortField = switch (sortField.toLowerCase()) {
            case "fecha", "fechacreacion" -> "fechaCreacion";
            case "titulo" -> "titulo";
            case "estado" -> "estado";
            case "ubicacion" -> "ubicacion";
            case "autor", "autornombre" -> "creador.fullName";
            default -> "fechaCreacion";
        };

        log.debug("Ordenando por campo: {} en dirección: {}", dbSortField, sortDirection);
        
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        Sort sort = Sort.by(sortDirection.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, dbSortField);

        return ResponseEntity.ok(incidenciaService.obtenerIncidenciasComunidadPaginadas(user, page, size, baseUrl, sort));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PRESIDENTE')")
    public ResponseEntity<IncidenciaResponse> actualizarEstadoIncidencia(
            @PathVariable Long id,
            @RequestBody String nuevoEstado,
            @AuthenticationPrincipal User user
    ) {
        try {
            EstadoIncidencia estado = EstadoIncidencia.valueOf(nuevoEstado.replace("\"", ""));
            Incidencia incidenciaActualizada = incidenciaService.actualizarEstadoIncidencia(id, estado, user);
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            return ResponseEntity.ok(incidenciaService.convertirADTO(incidenciaActualizada, baseUrl));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Estado no válido: " + nuevoEstado);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'PRESIDENTE', 'VECINO')")
    public ResponseEntity<Void> eliminarIncidencia(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        incidenciaService.eliminarIncidencia(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/download/**")
    public ResponseEntity<Resource> descargarImagen(HttpServletRequest request) {
        try {
            // Extraer el path completo después de /download/
            String path = request.getRequestURI();
            String fileName = path.substring(path.indexOf("/download/") + "/download/".length());
            
            log.info("=== DESCARGA DE IMAGEN ===");
            log.info("Archivo solicitado: {}", fileName);
            log.info("Request URI: {}", path);
            
            // El fileName que viene ya puede incluir "incidencias/" o no
            // Si fileName es "incidencias/archivo.png", usar directamente
            // Si fileName es solo "archivo.png", añadir "incidencias/"
            String resourcePath;
            if (fileName.startsWith("incidencias/")) {
                resourcePath = fileName; // Ya incluye el subdirectorio
            } else {
                resourcePath = "incidencias/" + fileName; // Añadir el subdirectorio
            }
            
            log.info("Ruta del recurso: {}", resourcePath);
            
            Resource resource = fileStorageService.loadFileAsResource(resourcePath);
            String contentType = determineContentType(fileName);
            
            // Para imágenes, usar "inline" en lugar de "attachment" para mostrarlas en el navegador
            String contentDisposition = contentType.startsWith("image/") ? 
                "inline" : "attachment";
            
            log.info("Imagen encontrada. Tipo: {}, Disposición: {}", contentType, contentDisposition);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            contentDisposition + "; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                    .body(resource);
        } catch (Exception e) {
            log.error("=== ERROR EN DESCARGA ===");
            log.error("Error al descargar la imagen {}: {}", request.getRequestURI(), e.getMessage(), e);
            throw new RuntimeException("Error al descargar la imagen: " + request.getRequestURI());
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }
}
