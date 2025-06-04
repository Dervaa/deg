package com.rzd.infra.test.repository;

import com.rzd.infra.test.entity.MongoBatch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Репозиторий Spring Data Mongo для коллекции "batches".
 */
public interface MongoBatchRepository extends MongoRepository<MongoBatch, String> {

    /** Найти документ по batchName */
    Optional<MongoBatch> findByBatchName(String batchName);
}
