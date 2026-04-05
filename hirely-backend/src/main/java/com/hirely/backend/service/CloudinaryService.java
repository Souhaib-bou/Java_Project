package com.hirely.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hirely.backend.config.CloudinaryConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;

    public CloudinaryService(Cloudinary cloudinary, CloudinaryConfig cloudinaryConfig) {
        this.cloudinary = cloudinary;
        this.cloudinaryConfig = cloudinaryConfig;
    }

    /**
     * Uploads a file to Cloudinary into: {folderRoot}/{taskId}/
     * Returns a small DTO-like map of useful values (secureUrl, publicId, etc.).
     */
    public UploadResult uploadTaskFile(MultipartFile file, int taskId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String folder = cloudinaryConfig.getFolderRoot() + "/" + taskId;

        // If you upload PDFs/DOCX/ZIP etc, set resource_type=auto
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto",
                        // optional: avoid name collisions
                        "public_id", UUID.randomUUID().toString()
                )
        );

        String secureUrl = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");
        String format = (String) result.get("format"); // may be null for some types

        return new UploadResult(
                secureUrl,
                publicId,
                file.getOriginalFilename(),
                file.getContentType(),
                format
        );
    }

    /**
     * Deletes an asset by public_id. Safe to call even if already deleted (Cloudinary returns result info).
     */
    public Map<?, ?> deleteByPublicId(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("publicId is blank");
        }

        // For non-image assets, Cloudinary may require resource_type=raw/video.
        // If you always upload with resource_type=auto, deletion may still need correct type.
        // Start with "image"; if you store PDFs etc, we’ll improve this later.
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // Small return object (cleaner than returning Map all over your code)
    public record UploadResult(
            String secureUrl,
            String publicId,
            String originalFileName,
            String contentType,
            String format
    ) {}
}