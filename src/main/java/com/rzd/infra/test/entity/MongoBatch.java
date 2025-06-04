package com.rzd.infra.test.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Документ “batch” (партия) в Mongo. Коллекция называется "batches".
 * Не путать с JPA-Entity (Postgres). Этот класс работает только с Mongo.
 */
@Document(collection = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MongoBatch {
    /** MongoDB ID (ObjectId), хранится автоматически */
    @Id
    private String id;

    /** Имя партии (yyyymmdd) – этот же batchName в Postgres */
    private String batchName;

    /** GridFS ID “сырая” ZIP-партия */
    private String rawZipId;

    /** GridFS ID “обработанная” ZIP-партия (после Python) */
    private String processedZipId;

    /**
     * (Опционально) Вложенный список всех строк CSV:
     * Вместо имени, latitude, longitude, height, roll, pitch, heading.
     * Если не нужно – можно оставить null.
     */
    private List<BatchCoord> csvCoordinates;

    /**
     * Внутренний вложенный класс – одна строка CSV
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchCoord {
        private String imageName;
        private Double latitude;
        private Double longitude;
        private Double height;
        private Double roll;
        private Double pitch;
        private Double heading;
    }
}
