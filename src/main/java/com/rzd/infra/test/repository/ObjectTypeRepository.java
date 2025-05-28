package com.rzd.infra.test.repository;

import com.rzd.infra.test.entity.ObjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObjectTypeRepository extends JpaRepository<ObjectType, Long> {
    Optional<ObjectType> findByName(String name);
}
