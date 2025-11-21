package com.winten.greenlight.admin.support.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String regionString;

    // Presigned URL 생성기
    private S3Presigner getPresigner() {
        return S3Presigner.builder()
                .region(Region.of(regionString))
                .credentialsProvider(DefaultCredentialsProvider.create()) // ~/.aws/credentials 자동 인식
                .build();
    }

    public Map<String, String> getPresignedUrl(String filename) {
        // 폴더 어디로 갈지 정하는곳
        String key = "action-groups/" + UUID.randomUUID() + "_" + filename; // 파일명 중복 방지

        S3Presigner presigner = getPresigner();

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/png") // 필요시 동적으로 변경
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // 링크 유효 시간 10분
                .putObjectRequest(objectRequest)
                .build();

        String presignedUrl = presigner.presignPutObject(presignRequest).url().toString();
        String fileUrl = "https://" + bucketName + ".s3." + regionString + ".amazonaws.com/" + key;

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        response.put("imageUrl", fileUrl);

        presigner.close();
        return response;
    }
}