package com.rzd.infra.test.controller;

import com.rzd.infra.test.service.BatchUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchUploadService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Void> upload(@RequestPart("file") MultipartFile zip) throws Exception {
        service.handle(zip);
        return ResponseEntity.ok().build();           // 200 OK
    }
}
