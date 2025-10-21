package com.campusmarket.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadGoodsImage(MultipartFile file);

    String uploadChatImage(MultipartFile file);
}
