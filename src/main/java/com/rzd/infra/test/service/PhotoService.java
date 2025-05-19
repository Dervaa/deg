package com.rzd.infra.test.service;

import com.rzd.infra.test.entity.Photo;
import com.rzd.infra.test.repository.PhotoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PhotoService {
    private final PhotoRepository repo;

    public PhotoService(PhotoRepository repo) {
        this.repo = repo;
    }

    public Photo create(Photo photo) {
        return repo.save(photo);
    }

    public Optional<Photo> get(Long id) {
        return repo.findById(id);
    }

    public List<Photo> listByObject(Long objectId) {
        return repo.findByObjectId(objectId);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
