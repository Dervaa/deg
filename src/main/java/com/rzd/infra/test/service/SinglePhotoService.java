package com.rzd.infra.test.service;

import com.rzd.infra.test.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SinglePhotoService {

    private final GridFsTemplate gridFs;
    private final DbSaver dbSaver;

    public void handle(MultipartFile file) throws Exception {
        // 1) кладём оригинал
        String gridId = gridFs.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()).toString();

        // 2) распознаём
        StubDetector.DetectionResult dr = StubDetector.detect(file.getResource().getFile());
        if (!dr.hasObject) return;                    // можно вернуть 204 No Content

        // 3) EXIF
        ExifUtil.ExifData ex = ExifUtil.read(file.getResource().getFile());

        // 4) в БД
        //        dbSaver.save(file.getResource().getFile(), gridId, null, ex, dr);
    }
}
