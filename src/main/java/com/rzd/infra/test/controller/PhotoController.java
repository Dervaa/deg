// src/main/java/com/rzd/infra/test/controller/PhotoController.java
package com.rzd.infra.test.controller;

import com.rzd.infra.test.service.SinglePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final SinglePhotoService service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadOne(@RequestPart("file") MultipartFile file) throws Exception {
        service.handle(file);
        return ResponseEntity.ok().build();
    }
}
