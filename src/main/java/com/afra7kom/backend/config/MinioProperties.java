package com.afra7kom.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private boolean enabled = false;
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "afra7kom";
    /** Base publique (ex. https://api.afra7kom.ma) — nginx proxifie /{bucket}/ vers MinIO */
    private String publicUrl = "http://localhost:9000";
    /** Si true, en cas d'échec MinIO on écrit sur disque (/uploads). En prod: false. */
    private boolean fallbackLocal = true;
}
