package com.tadobasolutions.utilities;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tadobasolutions.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;

    public record UploadResult(String url, String publicId) {}

    public UploadResult uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Image size must be 5MB or less");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );
            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id")
            );
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
        }
    }

    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            // Don't fail the request if the old image cleanup fails
            log.warn("Failed to delete Cloudinary image {}", publicId, e);
        }
    }
}