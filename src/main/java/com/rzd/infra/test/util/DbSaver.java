package com.rzd.infra.test.util;

import com.rzd.infra.test.entity.InfrastructureObject;
import com.rzd.infra.test.entity.Photo;
import com.rzd.infra.test.repository.InfrastructureObjectRepository;
import com.rzd.infra.test.repository.ObjectTypeRepository;
import com.rzd.infra.test.repository.PhotoRepository;
import com.rzd.infra.test.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Компонент для сохранения одной фотографии и соответствующего инфраструктурного объекта в БД.
 * Использует координаты из сырного CSV (latitude, longitude), а не Exif.
 * Логика уверенности (confidence) приведена к формату 0.0–1.0: порог 0.5.
 */
@Component
@RequiredArgsConstructor
public class DbSaver {

    private final PhotoRepository photoRepo;
    private final InfrastructureObjectRepository objRepo;
    private final StatusRepository statusRepo;
    private final ObjectTypeRepository typeRepo;

    /**
     * GeometryFactory как static final: не создаём каждый раз новый.
     */
    private static final GeometryFactory GF = new GeometryFactory();

    /**
     * Сохраняет одну фотографию и один объект инфраструктуры.
     *
     * @param imgFile     файл изображения (используется, если нужно что-то с локальной FS; в нашем случае не читаем его содержимое)
     * @param gridFsId    идентификатор загруженного в GridFS изображения (JPEG с красным боксом)
     * @param batchName   строковый ключ партии (например "20250525")
     * @param latitude    широта, взятая из сырного CSV
     * @param longitude   долгота, взятая из сырного CSV
     * @param objectType  название типа объекта (например "power pole"); должно существовать в таблице object_type
     * @param confidence  уверенность модели в формате 0.0–1.0 (например 0.72)
     */
    @Transactional
    public void save(
            java.io.File imgFile,
            String gridFsId,
            String batchName,
            double latitude,
            double longitude,
            String objectType,
            double confidence
    ) {
        // 1) Собираем точку из координат CSV
        //    Если latitude/longitude равны 0.0, можно задать дефолтную точку или бросить ошибку
        Point point = GF.createPoint(new Coordinate(longitude, latitude));

        // 2) Сохраняем Photo
        //    Поле path оставляем под gridFsId (идентификатор в GridFS)
        Photo photo = Photo.builder()
                .path(gridFsId)
                .batchName(batchName)
                .geom(point)
                .shotTime(null)         // EXIF-парсер больше не нужен
                .source("raw-csv")      // отмечаем, что координаты взяты из CSV
                .resolution(null)       // можно при необходимости заполнить, если доступно
                .confidence(confidence) // confidence в формате 0.0–1.0
                .build();
        photo = photoRepo.save(photo);

        // 3) Определяем статус: порог 0.5 (50%)
        String statusName = (confidence < 0.5) ? "ТРЕБУЕТ УТОЧНЕНИЯ" : "ГОТОВ";

        // 4) Создаём InfrastructureObject, записываем gridFsId как photoId
        InfrastructureObject obj = InfrastructureObject.builder()
                .photoId(gridFsId)
                .geom(point)
                .confidence(confidence)
                .type(typeRepo.findByName(objectType)
                        .orElseThrow(() -> new IllegalArgumentException("Неизвестный тип объекта: " + objectType)))
                .status(statusRepo.findByName(statusName)
                        .orElseThrow(() -> new IllegalStateException("Отсутствует статус: " + statusName)))
                .build();
        objRepo.save(obj);

        // 5) Связываем Photo → InfrastructureObject (обновляем поле object_id в photo)
        photo.setObject(obj);
        photoRepo.save(photo);
    }
}
