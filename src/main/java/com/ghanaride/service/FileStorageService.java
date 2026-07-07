package com.ghanaride.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Handles all file upload and storage operations.
 *
 * Stores files on the local filesystem under the
 * configured upload directories.
 *
 * Directory structure:
 * uploads/
 * ├── cars/          ← Car/vehicle images
 * ├── profiles/      ← User profile photos
 * └── documents/     ← Driver verification docs
 *                       (ID, license — protected)
 *
 * Production note:
 * For scalability, replace local storage with
 * cloud storage (AWS S3, Cloudflare R2, or
 * DigitalOcean Spaces). I've included the
 * structure to make that migration easy.
 *
 * Railway note:
 * Railway's filesystem is ephemeral — files are
 * lost on redeploy. For production on Railway,
 * you MUST use cloud storage (S3/R2).
 * See the NOTE below.
 */
@Slf4j
@Service
public class FileStorageService {

    // =========================================================
    // CONFIGURATION
    // Values from application.properties
    // =========================================================
    @Value("${app.upload.dir:uploads/cars/}")
    private String carUploadDir;

    @Value("${app.upload.dir.profiles:uploads/profiles/}")
    private String profileUploadDir;

    private static final String DOCUMENTS_DIR =
            "uploads/documents/";

    // Max file sizes
    private static final long MAX_IMAGE_SIZE =
            5 * 1024 * 1024;       // 5MB for car images
    private static final long MAX_PROFILE_SIZE =
            2 * 1024 * 1024;       // 2MB for profile photos
    private static final long MAX_DOCUMENT_SIZE =
            10 * 1024 * 1024;      // 10MB for documents (PDF/image)

    // Allowed types per category
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/webp"
            );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/webp",
                    "application/pdf"
            );

    // =========================================================
    // INITIALIZATION
    // Creates upload directories on startup if they don't exist
    // =========================================================
    @PostConstruct
    public void init() {
        createDirectoryIfNotExists(carUploadDir);
        createDirectoryIfNotExists(profileUploadDir);
        createDirectoryIfNotExists(DOCUMENTS_DIR);
        log.info(
                "FileStorageService initialized. " +
                        "Upload dirs: cars={}, profiles={}, " +
                        "documents={}",
                carUploadDir, profileUploadDir,
                DOCUMENTS_DIR
        );
    }

    // =========================================================
    // STORE CAR IMAGE
    // Called from DriverController when adding a trip
    // =========================================================
    public String storeFile(MultipartFile file) {
        return storeCarImage(file);
    }

    public String storeCarImage(MultipartFile file) {
        validateFile(
                file,
                ALLOWED_IMAGE_TYPES,
                MAX_IMAGE_SIZE,
                "Car image"
        );
        return store(file, carUploadDir);
    }

    // =========================================================
    // STORE PROFILE IMAGE
    // Called from ProfileController
    // =========================================================
    public String storeProfileImage(MultipartFile file) {
        validateFile(
                file,
                ALLOWED_IMAGE_TYPES,
                MAX_PROFILE_SIZE,
                "Profile image"
        );
        return store(file, profileUploadDir);
    }

    // =========================================================
    // STORE DRIVER DOCUMENT
    // Called when driver uploads Ghana Card, license, etc.
    // These are stored in a PROTECTED directory
    // (not publicly accessible via static URL)
    // =========================================================
    public String storeDocument(MultipartFile file) {
        validateFile(
                file,
                ALLOWED_DOCUMENT_TYPES,
                MAX_DOCUMENT_SIZE,
                "Document"
        );
        return store(file, DOCUMENTS_DIR);
    }

    // =========================================================
    // DELETE FILE
    // Called when user changes profile pic or car image
    // =========================================================
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }

        try {
            // Remove leading slash if present
            String cleanPath = filePath.startsWith("/")
                    ? filePath.substring(1)
                    : filePath;

            Path path = Paths.get(cleanPath)
                    .toAbsolutePath()
                    .normalize();

            // Security: prevent path traversal
            // File must be within our upload directories
            if (!isWithinUploadDirectory(path)) {
                log.warn(
                        "Attempted to delete file outside " +
                                "upload directory: {}",
                        filePath
                );
                return false;
            }

            boolean deleted = Files.deleteIfExists(path);

            if (deleted) {
                log.info(
                        "File deleted: {}", cleanPath
                );
            }

            return deleted;

        } catch (IOException e) {
            log.error(
                    "Failed to delete file: {}",
                    filePath, e
            );
            return false;
        }
    }

    // =========================================================
    // CHECK IF FILE EXISTS
    // =========================================================
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        String cleanPath = filePath.startsWith("/")
                ? filePath.substring(1) : filePath;
        return Files.exists(Paths.get(cleanPath));
    }

    // =========================================================
    // GET FILE SIZE
    // =========================================================
    public long getFileSize(String filePath) {
        try {
            String cleanPath = filePath.startsWith("/")
                    ? filePath.substring(1) : filePath;
            return Files.size(Paths.get(cleanPath));
        } catch (IOException e) {
            return 0;
        }
    }

    // =========================================================
    // CORE STORE METHOD
    // All upload methods delegate here
    // =========================================================
    private String store(
            MultipartFile file,
            String directory
    ) {
        try {
            // Get original filename safely
            String originalFilename =
                    file.getOriginalFilename();

            // Extract extension
            String extension = getFileExtension(
                    originalFilename
            );

            // Generate unique filename
            // (prevents overwriting existing files
            //  and path traversal attacks)
            String uniqueFilename =
                    UUID.randomUUID().toString() +
                            "." + extension;

            // Resolve full path safely
            Path uploadPath = Paths.get(directory)
                    .toAbsolutePath()
                    .normalize();

            Path targetPath = uploadPath
                    .resolve(uniqueFilename)
                    .normalize();

            // Security check: ensure target is within
            // the intended directory
            if (!targetPath.startsWith(uploadPath)) {
                throw new IllegalArgumentException(
                        "Invalid file path detected"
                );
            }

            // Ensure directory exists
            Files.createDirectories(uploadPath);

            // Write file
            try (InputStream inputStream =
                         file.getInputStream()) {
                Files.copy(
                        inputStream,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            // Return relative path for storage in DB
            String relativePath = directory +
                    uniqueFilename;

            log.info(
                    "File stored: {} size={}KB type={}",
                    relativePath,
                    file.getSize() / 1024,
                    file.getContentType()
            );

            return relativePath;

        } catch (IOException e) {
            log.error(
                    "Failed to store file in {}: {}",
                    directory, e.getMessage(), e
            );
            throw new RuntimeException(
                    "Failed to save file. " +
                            "Please try again.", e
            );
        }
    }

    // =========================================================
    // VALIDATION
    // Validates file type and size before storing
    // =========================================================
    private void validateFile(
            MultipartFile file,
            Set<String> allowedTypes,
            long maxSize,
            String fileLabel
    ) {
        // Null check
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    fileLabel + " cannot be empty."
            );
        }

        // File size check
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    fileLabel + " is too large. " +
                            "Maximum size: " +
                            (maxSize / (1024 * 1024)) + "MB. " +
                            "Your file: " +
                            (file.getSize() / (1024 * 1024)) + "MB."
            );
        }

        // MIME type check
        String contentType = file.getContentType();
        if (contentType == null ||
                !allowedTypes.contains(
                        contentType.toLowerCase()
                )) {
            throw new IllegalArgumentException(
                    fileLabel + " type not allowed. " +
                            "Allowed types: " +
                            String.join(", ", allowedTypes) +
                            ". Received: " + contentType
            );
        }

        // Filename check (prevent path traversal)
        String originalFilename =
                file.getOriginalFilename();
        if (originalFilename != null) {
            if (originalFilename.contains("..") ||
                    originalFilename.contains("/") ||
                    originalFilename.contains("\\")) {
                throw new IllegalArgumentException(
                        "Invalid filename: " +
                                originalFilename
                );
            }
        }

        // Check file extension matches content type
        // (prevents disguising malicious files)
        if (originalFilename != null &&
                !originalFilename.isEmpty()) {
            String extension = getFileExtension(
                    originalFilename
            ).toLowerCase();

            boolean extensionMatchesType =
                    switch (contentType.toLowerCase()) {
                        case "image/jpeg",
                             "image/jpg" ->
                                extension.equals("jpg") ||
                                        extension.equals("jpeg");
                        case "image/png" ->
                                extension.equals("png");
                        case "image/webp" ->
                                extension.equals("webp");
                        case "application/pdf" ->
                                extension.equals("pdf");
                        default -> false;
                    };

            if (!extensionMatchesType) {
                log.warn(
                        "File extension mismatch: " +
                                "name={} type={}",
                        originalFilename, contentType
                );
                // Log warning but don't reject —
                // extension spoofing isn't always
                // malicious (some systems rename files)
            }
        }
    }

    // =========================================================
    // HELPER: Extract file extension
    // =========================================================
    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "bin";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 ||
                dotIndex == filename.length() - 1) {
            return "bin";
        }
        String ext = filename.substring(dotIndex + 1)
                .toLowerCase()
                .trim();

        // Whitelist allowed extensions
        return Set.of(
                "jpg", "jpeg", "png", "webp", "pdf"
        ).contains(ext) ? ext : "bin";
    }

    // =========================================================
    // HELPER: Security check — file within upload dir
    // =========================================================
    private boolean isWithinUploadDirectory(Path path) {
        try {
            Path carDir = Paths.get(carUploadDir)
                    .toAbsolutePath().normalize();
            Path profileDir = Paths.get(profileUploadDir)
                    .toAbsolutePath().normalize();
            Path docDir = Paths.get(DOCUMENTS_DIR)
                    .toAbsolutePath().normalize();

            return path.startsWith(carDir) ||
                    path.startsWith(profileDir) ||
                    path.startsWith(docDir);
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // HELPER: Create directory if it doesn't exist
    // =========================================================
    private void createDirectoryIfNotExists(
            String directoryPath
    ) {
        try {
            Path path = Paths.get(directoryPath)
                    .toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info(
                        "Created upload directory: {}",
                        path
                );
            }
        } catch (IOException e) {
            log.error(
                    "Failed to create upload directory: {}",
                    directoryPath, e
            );
            throw new RuntimeException(
                    "Cannot initialize upload directory: " +
                            directoryPath, e
            );
        }
    }
}