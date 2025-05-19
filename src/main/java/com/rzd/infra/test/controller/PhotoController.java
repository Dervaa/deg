package com.rzd.infra.test.controller;

import com.rzd.infra.test.entity.Photo;
import com.rzd.infra.test.service.PhotoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/photos")
public class PhotoController {
    private final PhotoService svc;

    public PhotoController(PhotoService svc) {
        this.svc = svc;
    }

    @GetMapping("/by-object/{objectId}")
    public List<Photo> listByObject(@PathVariable Long objectId) {
        return svc.listByObject(objectId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Photo> get(@PathVariable Long id) {
        return svc.get(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Photo> create(@RequestBody Photo photo) {
        Photo saved = svc.create(photo);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }
}
