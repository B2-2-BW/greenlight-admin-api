package com.winten.greenlight.admin.api.controller.support;

import com.winten.greenlight.admin.support.util.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {
    private final S3Service s3Service;

    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl(@RequestParam String filename) {
        return ResponseEntity.ok(s3Service.getPresignedUrl(filename));
    }
}