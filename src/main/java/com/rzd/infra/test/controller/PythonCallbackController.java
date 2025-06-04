package com.rzd.infra.test.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.rzd.infra.test.service.PythonCallbackService;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class PythonCallbackController {

    private final PythonCallbackService pythonCallbackService;

    /**
     * Принимает «обработанный» ZIP от Python-воркера.
     * Имя файла должно быть в формате "<batchName>_processed.zip", например, "20250525_processed.zip",
     * где batchName = "20250525".
     *
     * @param zipFile Обработанный ZIP-файл, отправленный Python-воркером.
     * @return ResponseEntity с результатом обработки.
     */
    @PostMapping("/processed")
    public ResponseEntity<?> receiveProcessedBatch(@RequestPart("file") MultipartFile zipFile) {
        log.info("=== Начало обработки запроса POST /api/v1/batch/processed ===");

        String originalFilename = zipFile.getOriginalFilename();;
        try {
            originalFilename = zipFile.getOriginalFilename();
            if (originalFilename == null) {
                log.error("Имя файла не указано в MultipartFile");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Имя файла не указано");
            }
            log.info("Получен POST /api/v1/batch/processed: файл='{}', размер={} байт", originalFilename, zipFile.getSize());

            pythonCallbackService.handleProcessedBatch(zipFile);
            log.info("Файл '{}' успешно обработан PythonCallbackService", originalFilename);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException iae) {
            log.warn("Некорректное имя файла '{}': {}", originalFilename, iae.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Невалидное имя архива: " + iae.getMessage());

        } catch (IllegalStateException ise) {
            String errorMessage = ise.getMessage() != null ? ise.getMessage() : "Неизвестная ошибка";
            if (errorMessage.contains("Batch не найден в Mongo")) {
                log.warn("Batch не найден при обработке файла '{}': {}", originalFilename, errorMessage);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("Batch не найден: " + errorMessage);
            } else {
                log.error("Ошибка при обработке файла '{}': {}", originalFilename, errorMessage, ise);
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Ошибка при обработке: " + errorMessage);
            }

        } catch (Exception ex) {
            log.error("Неожиданная ошибка при обработке ZIP '{}': {}", originalFilename, ex.getMessage(), ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ошибка при обработке ZIP: " + ex.getMessage());
        }
    }
}