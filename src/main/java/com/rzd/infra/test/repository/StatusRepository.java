package com.rzd.infra.test.repository;

import com.rzd.infra.test.entity.ObjectType;
import com.rzd.infra.test.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByName(String name);
}