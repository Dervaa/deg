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

import java.io.File;

@Component
@RequiredArgsConstructor
public class DbSaver {

    private final PhotoRepository                photoRepo;
    private final InfrastructureObjectRepository objRepo;
    private final StatusRepository               statusRepo;
    private final ObjectTypeRepository           typeRepo;

    // GeometryFactory можно бить синглтоном
    private final GeometryFactory gf = new GeometryFactory();

    /**
     * Сохраняет одну фотку и один объект.
     * InfrastructureObject.photoId хранит gridFsId (а не всю сущность Photo).
     */
    @Transactional
    public void save(File img,
              String gridFsId,
              String batchName,
              ExifUtil.ExifData ex,
              String objectType,
              double confidence) {

        // 1) Собираем точку
        double lon = ex != null && ex.lon != null ? ex.lon : 37.6173;
        double lat = ex != null && ex.lat != null ? ex.lat : 55.7558;
        Point point = gf.createPoint(new Coordinate(lon, lat));

        // 2) Сохраняем Photo
        Photo photo = Photo.builder()
                .path(gridFsId)
                .batchName(batchName)
                .geom(point)
                .shotTime(ex != null ? ex.shotTime : null)
                .resolution(ex != null && ex.width != null && ex.height != null
                        ? ex.width + "x" + ex.height
                        : null)
                .confidence(confidence)
                .build();
        photo = photoRepo.save(photo);

        String statusName = confidence < 70 ? "ТРЕБУЕТ УТОЧНЕНИЯ" : "ГОТОВ";
        // 3) Сохраняем InfrastructureObject, кладём в него photoId
        InfrastructureObject obj = InfrastructureObject.builder()
                .photoId(photo.getPath())
                .geom(point)
                .confidence(confidence)
                .type(typeRepo.findByName(objectType)
                        .orElseThrow(() -> new IllegalArgumentException("Неизвестный тип: " + objectType)))
                .status(statusRepo.findByName(statusName)
                        .orElseThrow(() -> new IllegalStateException("Отсутствует статус: " + statusName)))
                .build();
        objRepo.save(obj);

        // 4) Если нужно, можно связать Photo → object_id и сохранить:
        photo.setObject(obj);
        photoRepo.save(photo);
    }
}
