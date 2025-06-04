package com.rzd.infra.test.controller;

import com.rzd.infra.test.service.BatchUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final BatchUploadService batchUploadService;

    /**
     * Принимает «сырое» ZIP (raw batch), имя которого используется как batchName.
     * Ожидаем, что файл назван "<batchName>.zip" (только цифры, длина 8 или 14).
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadRawBatch(
            @RequestPart("file") MultipartFile zipFile
    ) {
        String originalFilename = zipFile.getOriginalFilename();
        log.info("Получен POST /api/v1/batch/upload: файл='{}'", originalFilename);

        try {
            batchUploadService.handleRawBatch(zipFile);
            log.info("Raw ZIP '{}' сохранён и отправлен на Python-воркер.", originalFilename);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException iae) {
            log.warn("Некорректное имя raw-архива '{}': {}", originalFilename, iae.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Невалидное имя архива: " + iae.getMessage());

        } catch (Exception e) {
            log.error("Ошибка загрузки raw ZIP '{}' в BatchUploadService: ", originalFilename, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка загрузки raw ZIP: " + e.getMessage());
        }
    }
}
