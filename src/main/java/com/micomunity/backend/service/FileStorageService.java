package com.micomunity.backend.service;

import com.micomunity.backend.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {
    private final Path rootLocation;
    
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("No se pudo inicializar el directorio de almacenamiento", e);
        }
    }

    public String storeFile(MultipartFile file) {
        return storeFile(file, "");
    }

    public String storeFile(MultipartFile file, String subdirectory) {
        try {
            if (file == null || file.isEmpty()) {
                throw new StorageException("El archivo está vacío");
            }

            String contentType = file.getContentType();
            log.debug("Almacenando archivo. Nombre: {}, Tipo: {}, Tamaño: {}", 
                     file.getOriginalFilename(), contentType, file.getSize());

            Path targetDir = subdirectory.isEmpty() ? rootLocation : rootLocation.resolve(subdirectory);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String fileName = generateUniqueFileName(file);
            Path targetLocation = targetDir.resolve(fileName);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Archivo almacenado exitosamente en: {}", targetLocation);
            
            return subdirectory.isEmpty() ? fileName : subdirectory + "/" + fileName;
        } catch (IOException e) {
            log.error("Error al almacenar archivo: {}", e.getMessage());
            throw new StorageException("Error almacenando archivo " + file.getOriginalFilename(), e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path file = rootLocation.resolve(fileName);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Error eliminando archivo " + fileName, e);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = rootLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException("No se pudo encontrar el archivo: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Error al cargar el archivo: " + fileName, e);
        }
    }

    private String generateUniqueFileName(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return UUID.randomUUID().toString() + "." + extension;
    }
}
