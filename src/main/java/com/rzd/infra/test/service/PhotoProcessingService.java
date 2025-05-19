package com.rzd.infra.test.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.rzd.infra.test.entity.InfrastructureObject;
import com.rzd.infra.test.repository.InfrastructureObjectRepository;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.bson.types.ObjectId;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class PhotoProcessingService {

    private final GridFsTemplate gridFsTemplate;
    private final MongoTemplate mongoTemplate;
    private final InfrastructureObjectRepository objRepo;
    private final GeometryFactory     geometryFactory;

    public InfrastructureObject handleUpload(MultipartFile file) throws Exception {
        // 1) Сохраняем оригинал в GridFS
        DBObject meta = new BasicDBObject();
        meta.put("originalFilename", file.getOriginalFilename());
        ObjectId gridFsId = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                meta
        );

        // 2) Читаем EXIF
        double lat = 55.7558, lon = 37.6173;  // fallback: Москва
        Metadata md = ImageMetadataReader.readMetadata(file.getInputStream());
        GpsDirectory gps = md.getFirstDirectoryOfType(GpsDirectory.class);
        if (gps != null && gps.getGeoLocation() != null) {
            lat = gps.getGeoLocation().getLatitude();
            lon = gps.getGeoLocation().getLongitude();
        }

        // 3) Чистим метаданные + компрессим (необязательно)
        BufferedImage img = ImageIO.read(file.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(img)
                .size(1024, 1024)
                .outputQuality(0.8)
                .toOutputStream(baos);
        // При желании можете залить baos.toByteArray() в Mongo отдельно

        // 4) Сохраняем «черновик карточки»
        Point p = geometryFactory.createPoint(new Coordinate(lon, lat));
        InfrastructureObject obj = InfrastructureObject.builder()
                .photoId(gridFsId.toHexString())
                .objType("UNKNOWN")
                .status("PROCESSING")
                .geom(p)
                .confidence(0.0)
                .build();
        obj = objRepo.save(obj);

        // 5) Заглушка ML: сразу «распознаём»
        String detectedType = "turnout";   // stub
        double conf          = 0.75;       // stub

        obj.setObjType(detectedType);
        obj.setConfidence(conf);
        obj.setStatus("DONE");
        return objRepo.save(obj);
    }
}
