package com.rzd.infra.test.controller;

import com.rzd.infra.test.service.PythonClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;            // <-- добавили эту строку
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/python")
@RequiredArgsConstructor
@Tag(name = "Python Integration", description = "Методы для взаимодействия с Python-воркером")
@Slf4j                                   // <-- добавили эту аннотацию
public class PythonClientController {

    private final PythonClientService pythonClientService;

    @Operation(summary = "Отправить ZIP с фотографиями на Python-воркер")
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> sendToPython(@RequestPart("file") MultipartFile zip) {
        String originalFilename = zip.getOriginalFilename();
        log.info("Получен запрос [POST /api/v1/python/send]: файл='{}'", originalFilename);

        try {
            pythonClientService.sendZipToPython(zip);
            log.info("Успешно отправили '{}' на Python-воркер", originalFilename);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при отправке '{}' на Python-воркер: {}", originalFilename, e.getMessage(), e);
            // Можно вернуть 500, чтобы FastAPI увидел, что что-то упало:
            return ResponseEntity.status(500).build();
        }
    }
}
