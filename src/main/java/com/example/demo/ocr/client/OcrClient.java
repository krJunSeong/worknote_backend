package com.example.demo.ocr.client;

import org.springframework.web.multipart.MultipartFile;

public interface OcrClient {

    String extractText(MultipartFile image);
}
