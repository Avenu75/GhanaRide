package com.ghanaride.service;

import com.ghanaride.entity.*;
import com.ghanaride.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File Storage Service - Handles all file uploads securely.
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

    @PostConstruct
    public void init() {
        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(uploadDir).toAbsolutePath().normalize());
            Files.createDirectories(Paths.get(profileUploadDir).toAbsolutePath().normalize());
            Files.createDirectories(Paths.get("uploads/documents/drivers").toAbsolutePath().normalize());
            Files.createDirectories(Paths.get("uploads/documents/companies").toAbsolutePath().normalize());
        } catch (IOException e) {
            log.error("Could not create upload directories", e);
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    // =========================================================
    // CAR IMAGES
    // =========================================================

    public String storeCarImage(MultipartFile file) {
        return storeFile(file, Paths.get(uploadDir), "car");
    }

    public String storeCarImage(MultipartFile file, String prefix) {
        return storeFile(file, Paths.get(uploadDir), prefix);
    }

    // =========================================================
    // PROFILE IMAGES
    // =========================================================

    public String storeProfileImage(MultipartFile file) {
        return storeFile(file, Paths.get(profileUploadDir), "profile");
    }

    // =========================================================
    // DRIVER / COMPANY DOCUMENTS
    // =========================================================

    public String storeDriverDocument(MultipartFile file, String type) {
        return storeFile(file, Paths.get("uploads/documents/drivers"), "driver_" + type);
    }

    public String storeCompanyDocument(MultipartFile file, String type) {
        return storeFile(file, Paths.get("uploads/documents/companies"), "company_" + type);
    }

    // =========================================================
    // CORE STORAGE LOGIC
    // =========================================================

    private String storeFile(MultipartFile file, Path targetDir, String prefix) {
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
        Path targetPath = targetDir.resolve(filename).toAbsolutePath().normalize();
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored file: {} -> {}", file.getOriginalFilename(), targetPath);
            return targetDir.relativize(targetPath).toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Allowed: " + allowedTypes);
        }

        // Additional safety: check for malicious content
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
            if (!isPathSafe(path)) {
                log.warn("Attempted to delete file outside upload directory: {}", path);
                return;
            }
            Files.deleteIfExists(path);
            log.debug("Deleted file: {}", path);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", relativePath, e);
        }
    }

    private boolean isPathSafe(Path path) {
        try {
            Path carDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path profileDir = Paths.get(profileUploadDir).toAbsolutePath().normalize();
            Path docDir = Paths.get("uploads/documents").toAbsolutePath().normalize();

            return path.startsWith(carDir) || path.startsWith(profileDir) || path.startsWith(docDir);
        } catch (Exception e) {
            return false;
        }
    }

}