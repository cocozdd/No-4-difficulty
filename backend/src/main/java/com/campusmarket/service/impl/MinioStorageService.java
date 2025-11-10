package com.campusmarket.service.impl;

import com.campusmarket.config.MinioProperties;
import com.campusmarket.service.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MinioStorageService implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp"
    );
    private static final Map<String, String> CONTENT_TYPE_ALIASES = Map.of(
            "image/jpg", MediaType.IMAGE_JPEG_VALUE,
            "image/pjpeg", MediaType.IMAGE_JPEG_VALUE,
            "image/x-citrix-jpeg", MediaType.IMAGE_JPEG_VALUE,
            "image/x-png", MediaType.IMAGE_PNG_VALUE,
            "image/x-citrix-png", MediaType.IMAGE_PNG_VALUE
    );

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public String uploadGoodsImage(MultipartFile file) {
        return upload(file, properties.getBucket().getGoods(), "goods");
    }

    @Override
    public String uploadChatImage(MultipartFile file) {
        return upload(file, properties.getBucket().getChat(), "chat");
    }

    private String upload(MultipartFile file, String bucket, String folder) {
        validateFile(file);
        String objectName = buildObjectName(file, folder);

        try {
            byte[] data = file.getBytes();
            String contentType = detectContentType(file, data);
            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(inputStream, data.length, -1)
                                .contentType(contentType)
                                .build()
                );
                return buildObjectUrl(bucket, objectName);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upload file to storage", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds limit (5MB)");
        }
    }

    private String detectContentType(MultipartFile file, byte[] data) throws IOException {
        String detected = normalizeContentType(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(data)));
        if (StringUtils.hasText(detected) && ALLOWED_CONTENT_TYPES.contains(detected)) {
            return detected;
        }
        String fallback = normalizeContentType(file.getContentType());
        if (StringUtils.hasText(fallback) && ALLOWED_CONTENT_TYPES.contains(fallback)) {
            return fallback;
        }
        String extensionBased = contentTypeFromExtension(file.getOriginalFilename());
        if (extensionBased != null) {
            return extensionBased;
        }
        throw new IllegalArgumentException("Unsupported image type");
    }

    private String buildObjectName(MultipartFile file, String folder) {
        String extension = sanitizeExtension(file.getOriginalFilename());
        LocalDate now = LocalDate.now();
        return String.format(
                "%s/%d/%02d/%s.%s",
                folder,
                now.getYear(),
                now.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
    }

    private String sanitizeExtension(String originalFilename) {
        String ext = FilenameUtils.getExtension(originalFilename);
        if (!StringUtils.hasText(ext)) {
            return "jpg";
        }
        String lower = ext.toLowerCase();
        if ("jpeg".equals(lower) || "jpg".equals(lower)) {
            return "jpg";
        }
        if ("png".equals(lower)) {
            return "png";
        }
        if ("gif".equals(lower)) {
            return "gif";
        }
        if ("webp".equals(lower)) {
            return "webp";
        }
        return "jpg";
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        String lower = contentType.toLowerCase();
        return CONTENT_TYPE_ALIASES.getOrDefault(lower, lower);
    }

    private String contentTypeFromExtension(String originalFilename) {
        String ext = FilenameUtils.getExtension(originalFilename);
        if (!StringUtils.hasText(ext)) {
            return null;
        }
        switch (ext.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return MediaType.IMAGE_JPEG_VALUE;
            case "png":
                return MediaType.IMAGE_PNG_VALUE;
            case "gif":
                return MediaType.IMAGE_GIF_VALUE;
            case "webp":
                return "image/webp";
            default:
                return null;
        }
    }

    private String buildObjectUrl(String bucket, String objectName) {
        String endpoint = StringUtils.hasText(properties.getPublicEndpoint())
                ? properties.getPublicEndpoint()
                : properties.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("MinIO endpoint is not configured");
        }
        boolean hasScheme = endpoint.startsWith("http://") || endpoint.startsWith("https://");
        if (!hasScheme) {
            String scheme = properties.isSecure() ? "https://" : "http://";
            endpoint = scheme + endpoint;
        } else if (properties.isSecure() && endpoint.startsWith("http://")) {
            endpoint = endpoint.replaceFirst("^http://", "https://");
        }
        return String.format("%s/%s/%s", endpoint.replaceAll("/$", ""), bucket, encodePath(objectName));
    }

    private String encodePath(String objectName) {
        return objectName.replace(" ", "%20");
    }
}

