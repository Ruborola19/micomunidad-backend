package com.micomunity.backend.service;

import com.micomunity.backend.dto.DocumentoRequest;
import com.micomunity.backend.dto.DocumentoResponse;
import com.micomunity.backend.model.*;
import com.micomunity.backend.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final FileStorageService fileStorageService;

    @Value("${documento.allowed.extensions}")
    private List<String> allowedExtensions;

    @Value("${documento.max.file.size}")
    private long maxFileSize;

    @Transactional
    public void publicarDocumento(User user, DocumentoRequest request) {
        log.debug("Iniciando publicación de documento por usuario: {}", user.getEmail());
        validatePresidentRole(user);
        validateFiles(request.getArchivos());

        List<String> urls = request.getArchivos().stream()
                .map(file -> {
                    String storedFile = fileStorageService.storeFile(file, "documentos");
                    log.debug("Archivo almacenado: {}", storedFile);
                    return storedFile;
                })
                .collect(Collectors.toList());

        Documento doc = new Documento();
        doc.setComentario(request.getComentario());
        doc.setTipo(request.getTipo());
        doc.setFechaPublicacion(LocalDateTime.now());
        doc.setUrlsArchivos(urls);
        doc.setAutor(user);
        doc.setComunidad(user.getCommunity());

        documentoRepository.save(doc);
        log.info("Documento tipo {} publicado por usuario: {}", request.getTipo(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public Page<DocumentoResponse> obtenerDocumentos(
            User user,
            TipoDocumento tipo,
            Integer dia,
            Integer mes,
            Integer anio,
            Pageable pageable,
            String baseUrl
    ) {
        try {
            log.info("=== INICIANDO OBTENCIÓN DE DOCUMENTOS ===");
            log.info("Usuario: {}", user.getEmail());
            log.info("Tipo documento: {}", tipo);
            log.info("Filtros de fecha: dia={}, mes={}, anio={}", dia, mes, anio);
            log.info("Parámetros pageable: {}", pageable);

            // Verificar que el usuario tenga comunidad asignada
            Community userCommunity = user.getCommunity();
            if (userCommunity == null) {
                String error = "Usuario " + user.getEmail() + " no tiene comunidad asignada";
                log.error(error);
                throw new RuntimeException(error);
            }

            log.info("Comunidad del usuario: ID={}", userCommunity.getId());

            // Usar el método con filtros de fecha si se proporciona algún filtro
            Page<Documento> documentos;
            if (dia != null || mes != null || anio != null) {
                log.info("Aplicando filtros de fecha");
                documentos = documentoRepository.findByComunidadAndTipoWithDateFilters(
                        userCommunity, tipo, dia, mes, anio, pageable
                );
            } else {
                log.info("Sin filtros de fecha - obteniendo todos los documentos");
                documentos = documentoRepository.findByComunidadAndTipoOrderByFechaPublicacionDesc(
                        userCommunity, tipo, pageable
                );
            }

            log.info("Documentos encontrados: {}. Total elements: {}", 
                    documentos.getContent().size(), documentos.getTotalElements());
            
            return documentos.map(doc -> {
                try {
                    log.debug("Convirtiendo documento ID: {}", doc.getId());
                    return convertToResponse(doc, user, baseUrl);
                } catch (Exception e) {
                    log.error("Error al convertir documento {}: {}", doc.getId(), e.getMessage(), e);
                    throw new RuntimeException("Error al procesar documento " + doc.getId(), e);
                }
            });

        } catch (Exception e) {
            String error = String.format(
                    "Error al obtener documentos para usuario %s, tipo %s: %s",
                    user.getEmail(), tipo, e.getMessage()
            );
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }

    @Transactional
    public void eliminarDocumento(User user, Long documentoId) {
        log.debug("Intentando eliminar documento {} por usuario {}", documentoId, user.getEmail());
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        validateDeletionPermission(user, documento);

        documento.getUrlsArchivos().forEach(url -> {
            try {
                fileStorageService.deleteFile(url);
                log.debug("Archivo eliminado: {}", url);
            } catch (Exception e) {
                log.warn("Error al eliminar archivo {}: {}", url, e.getMessage());
            }
        });

        documentoRepository.delete(documento);
        log.info("Documento {} tipo {} eliminado por usuario: {}",
                documentoId, documento.getTipo(), user.getEmail());
    }

    private DocumentoResponse convertToResponse(Documento doc, User currentUser, String baseUrl) {
        try {
            log.debug("Convirtiendo documento ID: {} a DTO", doc.getId());
            DocumentoResponse response = new DocumentoResponse();
            response.setId(doc.getId());
            response.setComentario(doc.getComentario());
            response.setTipo(doc.getTipo());
            response.setFechaPublicacion(doc.getFechaPublicacion());

            // Manejo seguro del autor (puede ser lazy)
            try {
                User autor = doc.getAutor();
                if (autor != null) {
                    response.setAutorNombre(autor.getFullName());
                    response.setAutorEmail(autor.getEmail());
                    log.debug("Autor cargado: {}", autor.getFullName());
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar el autor del documento {}: {}", doc.getId(), e.getMessage());
                response.setAutorNombre("Usuario no disponible");
                response.setAutorEmail("");
            }

            // Construir URLs completas
            List<String> fullUrls = doc.getUrlsArchivos().stream()
                    .map(url -> baseUrl + "/api/documentos/download/" + url)
                    .collect(Collectors.toList());
            response.setUrlsArchivos(fullUrls);
            log.debug("URLs de archivos construidas: {}", fullUrls.size());

            response.setPuedeEliminar(canDeleteDocument(currentUser, doc));

            log.debug("Documento ID: {} convertido exitosamente", doc.getId());
            return response;
        } catch (Exception e) {
            log.error("Error al convertir documento ID: {} a DTO: {}", doc.getId(), e.getMessage(), e);
            throw new RuntimeException("Error al procesar el documento");
        }
    }

    private void validatePresidentRole(User user) {
        if (user.getRole() != Role.PRESIDENTE) {
            log.warn("Usuario {} intentó publicar documento sin ser presidente", user.getEmail());
            throw new RuntimeException("Solo el presidente puede publicar documentos");
        }
    }

    private void validateDeletionPermission(User user, Documento documento) {
        if (!canDeleteDocument(user, documento)) {
            log.warn("Usuario {} intentó eliminar documento {} sin permisos", user.getEmail(), documento.getId());
            throw new RuntimeException("No tienes permisos para eliminar este documento");
        }
    }

    private boolean canDeleteDocument(User user, Documento documento) {
        return user.getRole() == Role.PRESIDENTE &&
               documento.getAutor().getId().equals(user.getId());
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("Debe adjuntar al menos un archivo");
        }

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new RuntimeException("Nombre de archivo inválido");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!allowedExtensions.contains(extension)) {
                log.warn("Intento de subir archivo con extensión no permitida: {}", extension);
                throw new RuntimeException("Tipo de archivo no permitido: " + extension);
            }

            if (file.getSize() > maxFileSize) {
                log.warn("Intento de subir archivo que excede el tamaño máximo: {} bytes", file.getSize());
                throw new RuntimeException("El archivo excede el tamaño máximo permitido");
            }
        }
    }
}
