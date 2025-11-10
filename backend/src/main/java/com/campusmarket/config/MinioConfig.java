package com.campusmarket.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient.Builder builder = MinioClient.builder()
                .credentials(properties.getAccessKey(), properties.getSecretKey());
        String endpoint = properties.getEndpoint();
        if (endpoint.startsWith("http")) {
            builder.endpoint(endpoint);
        } else {
            builder.endpoint(endpoint, properties.isSecure() ? 443 : 80, properties.isSecure());
        }
        return builder.build();
    }

    @Bean
    public BucketInitializer bucketInitializer(MinioClient minioClient, MinioProperties properties) {
        return new BucketInitializer(minioClient, properties);
    }

    public static class BucketInitializer {
        public BucketInitializer(MinioClient minioClient, MinioProperties properties) {
            try {
                ensureBucket(minioClient, properties.getBucket().getGoods());
                ensureBucket(minioClient, properties.getBucket().getChat());
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to configure MinIO buckets", ex);
            }
        }

        private void ensureBucket(MinioClient minioClient, String bucketName) throws Exception {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            applyReadOnlyPolicy(minioClient, bucketName);
        }

        private void applyReadOnlyPolicy(MinioClient minioClient, String bucketName) throws Exception {
            String policy = String.format(
                    "{\n" +
                            "  \"Version\": \"2012-10-17\",\n" +
                            "  \"Statement\": [\n" +
                            "    {\n" +
                            "      \"Effect\": \"Allow\",\n" +
                            "      \"Principal\": {\n" +
                            "        \"AWS\": [\"*\"]\n" +
                            "      },\n" +
                            "      \"Action\": [\"s3:GetObject\"],\n" +
                            "      \"Resource\": [\"arn:aws:s3:::%s/*\"]\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}\n",
                    bucketName
            );
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
        }
    }
}
