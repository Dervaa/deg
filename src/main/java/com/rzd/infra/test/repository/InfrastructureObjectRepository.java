package com.rzd.infra.test.repository;

import com.rzd.infra.test.entity.InfrastructureObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfrastructureObjectRepository
        extends JpaRepository<InfrastructureObject, Long> {
    // здесь позже добавим spatial-методы (findByGeomNear …)
}
