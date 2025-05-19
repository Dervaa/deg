package com.rzd.infra.test.service;

import com.rzd.infra.test.entity.InfrastructureObject;
import com.rzd.infra.test.repository.InfrastructureObjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InfrastructureObjectService {
    private final InfrastructureObjectRepository repo;

    public InfrastructureObjectService(InfrastructureObjectRepository repo) {
        this.repo = repo;
    }

    public InfrastructureObject create(InfrastructureObject obj) {
        return repo.save(obj);
    }

    public Optional<InfrastructureObject> get(Long id) {
        return repo.findById(id);
    }

    public List<InfrastructureObject> list() {
        return repo.findAll();
    }

    public InfrastructureObject update(InfrastructureObject obj) {
        return repo.save(obj);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
