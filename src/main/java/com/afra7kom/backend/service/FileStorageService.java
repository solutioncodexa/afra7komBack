package com.afra7kom.backend.service;

import com.afra7kom.backend.config.MinioProperties;
import com.afra7kom.backend.exception.BadRequestException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg"
    );

    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    @Value("${file.upload-dir:./uploads/}")
    private String uploadDir;

    @Value("${file.image.max-width:1920}")
    private int maxWidth;

    @Value("${file.image.max-height:1920}")
    private int maxHeight;

    @Value("${file.image.quality:0.82}")
    private double imageQuality;

    @Value("${file.image.compress:true}")
    private boolean compressImages;

    public FileStorageService(MinioProperties minioProperties,
                              @Autowired(required = false) MinioClient minioClient) {
        this.minioProperties = minioProperties;
        this.minioClient = minioClient;
    }

    /**
     * Stocke une image (compression automatique) et retourne l'URL publique à enregistrer en base.
     * <ul>
     *   <li>MinIO : {@code {publicUrl}/{bucket}/{folder}/{uuid}.jpg}</li>
     *   <li>Local  : {@code /uploads/{folder}/{uuid}.jpg}</li>
     * </ul>
     */
    public String storeImage(MultipartFile file, String folder) throws IOException {
        validateImage(file);

        byte[] processed = compressImages ? compressImage(file) : file.getBytes();
        String objectName = buildObjectName(folder, "jpg");
        String contentType = compressImages ? "image/jpeg" : resolveContentType(file);

        if (minioProperties.isEnabled()) {
            if (minioClient == null) {
                if (!minioProperties.isFallbackLocal()) {
                    throw new IOException("MinIO activé mais client non initialisé");
                }
                log.warn("MinIO activé sans client — fallback local");
                return uploadLocally(processed, objectName);
            }
            try {
                return uploadToMinio(processed, objectName, contentType);
            } catch (Exception e) {
                if (!minioProperties.isFallbackLocal()) {
                    throw new IOException("Échec upload MinIO (fallback local désactivé): " + e.getMessage(), e);
                }
                log.warn("MinIO indisponible, stockage local utilisé: {}", e.getMessage());
            }
        }
        return uploadLocally(processed, objectName);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier image requis");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Type de fichier non supporté. Formats acceptés: JPEG, PNG, GIF, WEBP");
        }
    }

    private byte[] compressImage(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new BadRequestException("Fichier image invalide");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(image)
                    .size(maxWidth, maxHeight)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(imageQuality)
                    .toOutputStream(output);

            byte[] compressed = output.toByteArray();
            log.debug("Image compressée: {} -> {} octets", file.getSize(), compressed.length);
            return compressed;
        }
    }

    private String uploadToMinio(byte[] data, String objectName, String contentType) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .stream(stream, data.length, -1)
                            .contentType(contentType)
                            .build()
            );

            // URL publique via nginx: https://api.afra7kom.ma/afra7kom/images/....jpg
            String baseUrl = minioProperties.getPublicUrl().replaceAll("/$", "");
            String url = baseUrl + "/" + minioProperties.getBucket() + "/" + objectName;
            log.info("Image stockée sur MinIO: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Erreur upload MinIO: {}", e.getMessage());
            throw new IOException("Erreur lors de l'upload vers MinIO", e);
        }
    }

    private String uploadLocally(byte[] data, String objectName) throws IOException {
        Path target = Paths.get(uploadDir).resolve(objectName).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Files.write(target, data);
        return "/uploads/" + objectName.replace("\\", "/");
    }

    private String buildObjectName(String folder, String extension) {
        String safeFolder = folder.replaceAll("^/+", "").replaceAll("/+$", "");
        return safeFolder + "/" + UUID.randomUUID() + "." + extension;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null ? contentType : "application/octet-stream";
    }
}
