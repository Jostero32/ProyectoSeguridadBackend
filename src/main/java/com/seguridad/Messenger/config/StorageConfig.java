package com.seguridad.Messenger.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StorageConfig {

    private final StorageProperties props;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    @Bean
    public ApplicationRunner initBucket(MinioClient minioClient) {
        return args -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(props.getBucket()).build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(props.getBucket()).build());
                }
            } catch (Exception ex) {
                log.warn("No se pudo inicializar el bucket '{}' en MinIO", props.getBucket(), ex);
            }
        };
    }
}
