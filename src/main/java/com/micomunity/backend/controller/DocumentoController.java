package com.micomunity.backend.controller;

import com.micomunity.backend.dto.DocumentoRequest;
import com.micomunity.backend.dto.DocumentoResponse;
import com.micomunity.backend.model.TipoDocumento;
import com.micomunity.backend.model.User;
import com.micomunity.backend.service.DocumentoService;
import com.micomunity.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Slf4j
public class DocumentoController {

    private final DocumentoService documentoService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PRESIDENTE')")
    public ResponseEntity<?> publicarDocumento(
            @RequestParam("comentario") String comentario,
            @RequestParam("tipo") TipoDocumento tipo,
            @RequestParam("archivos") List<MultipartFile> archivos,
            @AuthenticationPrincipal User user,
            HttpServletRequest request
    ) {
        try {
            log.debug("Recibiendo solicitud para publicar documento. Tipo: {}, Archivos: {}",
                    tipo, archivos.stream().map(MultipartFile::getOriginalFilename).toList());

            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null)
                    .build()
                    .toUriString();

            DocumentoRequest docRequest = new DocumentoRequest(comentario, tipo, archivos);
            documentoService.publicarDocumento(user, docRequest);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al publicar documento: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<DocumentoResponse>> obtenerDocumentos(
            @AuthenticationPrincipal User user,
            @RequestParam TipoDocumento tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer dia,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio,
            HttpServletRequest request
    ) {
        try {
            log.info("Recibida petición GET /documentos con parámetros: tipo={}, page={}, size={}, dia={}, mes={}, anio={}",
                    tipo, page, size, dia, mes, anio);

            if (user == null || user.getCommunity() == null) {
                String error = "Usuario o comunidad no válidos";
                log.error(error);
                return ResponseEntity.badRequest().body(null);
            }

            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null)
                    .build()
                    .toUriString();

            // Usar PageRequest.of sin Sort ya que el orden está fijo en la consulta SQL
            PageRequest pageRequest = PageRequest.of(page, size);

            Page<DocumentoResponse> documentos = documentoService.obtenerDocumentos(
                    user, tipo, dia, mes, anio, pageRequest, baseUrl
            );

            log.info("Documentos obtenidos exitosamente. Total: {}", documentos.getTotalElements());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(documentos);

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error al obtener documentos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener documentos: " + e.getMessage(), e);
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable String fileName) {
        try {
            log.debug("Intentando descargar archivo: {}", fileName);
            Path filePath = Paths.get("uploads/documentos").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.error("Archivo no encontrado: {}", fileName);
                throw new RuntimeException("No se pudo encontrar el archivo: " + fileName);
            }
        } catch (Exception e) {
            log.error("Error al descargar el archivo {}: {}", fileName, e.getMessage(), e);
            throw new RuntimeException("Error al descargar el archivo: " + fileName, e);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRESIDENTE')")
    public ResponseEntity<?> eliminarDocumento(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        try {
            log.debug("Recibiendo solicitud para eliminar documento {}. Usuario: {}", id, user.getEmail());
            documentoService.eliminarDocumento(user, id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al eliminar documento {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc", "docx" -> "application/msword";
            case "xls", "xlsx" -> "application/vnd.ms-excel";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }
}
