package com.campusmarket.controller;

import com.campusmarket.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
public class FileUploadController {

    private final StorageService storageService;

    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/goods-image")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> uploadGoodsImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadGoodsImage(file);
        return Map.of("url", url);
    }

    @PostMapping("/chat-image")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> uploadChatImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadChatImage(file);
        return Map.of("url", url);
    }
}
