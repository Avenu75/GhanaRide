package com.ghanaride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * File Storage Service - Handles file uploads for avatars, car images, documents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads/cars/}")
    private String uploadDir;

    @Value("${app.upload.dir.profiles:uploads/profiles/}")
    private String profileUploadDir;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedTypes;

    @Value("${app.upload.max-file-size:5242880}")
    private long maxFileSize;

    private List<String> allowedTypeList;
    private Path carUploadPath;
    private Path profileUploadPath;
    private Path documentUploadPath;

    @PostConstruct
    public void init() {
        this.allowedTypeList = Arrays.asList(allowedTypes.split(","));
        
        this.carUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.profileUploadPath = Paths.get(profileUploadDir).toAbsolutePath().normalize();
        this.documentUploadPath = Paths.get("uploads/documents").toAbsolutePath().normalize();

        // Create directories
        createDirectories(carUploadPath);
        createDirectories(profileUploadPath);
        createDirectories(documentUploadPath);

        log.info("File storage initialized - Cars: {}, Profiles: {}, Documents: {}", 
            carUploadPath, profileUploadPath, documentUploadPath);
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", path, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    // =========================================================
    // CAR IMAGES
    // =========================================================

    public String storeCarImage(MultipartFile file) {
        return storeFile(file, carUploadPath, "car");
    }

    public String storeCarImage(MultipartFile file, String prefix) {
        return storeFile(file, carUploadPath, prefix);
    }

    // =========================================================
    // PROFILE IMAGES
    // =========================================================

    public String storeProfileImage(MultipartFile file) {
        return storeFile(file, profileUploadPath, "profile");
    }

    // =========================================================
    // DRIVER / COMPANY DOCUMENTS
    // =========================================================

    public String storeDriverDocument(MultipartFile file, String type) {
        return storeFile(file, documentUploadPath.resolve("drivers"), "driver_" + type);
    }

    public String storeCompanyDocument(MultipartFile file, String type) {
        return storeFile(file, documentUploadPath.resolve("companies"), "company_" + type);
    }

    // =========================================================
    // CORE STORAGE LOGIC
    // =========================================================

    private String storeFile(MultipartFile file, Path targetDir, String prefix) {
        // Validate
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        validateFile(file);

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String filename = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        // Ensure directory exists
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("Failed to create directory: {}", targetDir, e);
            throw new RuntimeException("Could not create upload directory", e);
        }

        // Save file
        Path targetPath = targetDir.resolve(filename);
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file: {} -> {}", originalFilename, targetPath);
            
            // Return relative path for database storage
            return targetDir.relativize(targetPath).toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        // Check size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypeList.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed. Allowed: " + allowedTypeList);
        }

        // Check filename
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "jpg";
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        
        try {
            Path path = Paths.get(relativePath).toAbsolutePath().normalize();
            // Security check - ensure it's within our upload directories
            if (path.startsWith(carUploadPath) || path.startsWith(profileUploadPath) || path.startsWith(documentUploadPath)) {
                Files.deleteIfExists(path);
                log.debug("Deleted file: {}", path);
            } else {
                log.warn("Attempted to delete file outside upload directories: {}", path);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }

    public void deleteCarImage(String relativePath) {
        delete(relativePath);
    }

    public void deleteProfileImage(String relativePath) {
        delete(relativePath);
    }
}