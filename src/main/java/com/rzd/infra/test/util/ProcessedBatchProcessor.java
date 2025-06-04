package com.rzd.infra.test.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessedBatchProcessor {

    private final GridFsTemplate gridFsTemplate;
    private final DbSaver        dbSaver;
    private final ObjectMapper   objectMapper = new ObjectMapper();

    /** Основной метод – вызывается из PythonCallbackService */
    public void processBatch(String batchName,
                             String rawZipId,
                             byte[] processedZipBytes) throws IOException {

        log.info("=== Начало обработки партии '{}' ===", batchName);
        log.debug("rawZipId: {}, тип: {}", rawZipId,
                rawZipId != null ? rawZipId.getClass().getName() : "null");

        /* ---------- валидация ---------- */
        if (rawZipId == null || rawZipId.trim().isEmpty() || !isValidObjectId(rawZipId)) {
            log.error("Некорректный rawZipId: {}", rawZipId);
            throw new IllegalArgumentException("rawZipId должен быть валидным ObjectId: " + rawZipId);
        }

        /* ---------- создаём временную папку ---------- */
        Path tmpRoot = Files.createTempDirectory("proc_" + batchName + "_");
        log.debug("Создана временная директория: {}", tmpRoot);

        try {
            /* === 1. скачиваем сырой ZIP из GridFS === */
            ObjectId rawObjId = new ObjectId(rawZipId);
            GridFSFile rawFile = gridFsTemplate.findOne(
                    Query.query(Criteria.where("_id").is(rawObjId)));
            if (rawFile == null) {
                throw new IllegalStateException("Не удалось получить сырой ZIP по rawZipId: " + rawZipId);
            }

            GridFsResource rawRes = gridFsTemplate.getResource(rawFile);
            if (rawRes == null || !rawRes.exists()) {
                throw new IllegalStateException("GridFsResource отсутствует для rawZipId: " + rawZipId);
            }

            byte[] rawZipBytes;
            try (InputStream ris = rawRes.getInputStream()) {
                rawZipBytes = IOUtils.toByteArray(ris);
            }
            log.debug("Скачано {} байт сырого ZIP", rawZipBytes.length);

            /* создаём каталоги raw / processed */
            Path rawDir  = tmpRoot.resolve("raw");
            Path procDir = tmpRoot.resolve("processed");
            Files.createDirectories(rawDir);
            Files.createDirectories(procDir);

            /* распаковываем сырой ZIP */
            log.info("Распаковываем сырой ZIP в '{}'", rawDir);
            unzip(rawZipBytes, rawDir);

            /* === 2. ищем и парсим CSV с координатами === */
            Path coordsCsv = findAnyCsvRecursively(rawDir);
            if (coordsCsv == null) {
                throw new IllegalStateException("CSV с координатами не найден в: " + rawDir);
            }
            Map<String, RawCoord> coordsMap = parseCoordsCsv(coordsCsv);
            log.info("Парсинг CSV завершён, строк: {}", coordsMap.size());

            /* === 3. распаковываем processed ZIP от Python === */
            log.info("Распаковываем processed ZIP ({} байт) в '{}'",
                    processedZipBytes.length, procDir);
            unzip(processedZipBytes, procDir);

            /* ищем detections_to_java.json */
            Path detectionsJson = findFileRecursively(procDir, "detections_to_java.json");
            if (detectionsJson == null) {
                throw new IllegalStateException("detections_to_java.json не найден в " + procDir);
            }
            List<DetectionJsonEntry> detections = parseDetectionsJson(detectionsJson);
            log.info("Нашли {} детекций", detections.size());

            Path imagesFinalDir = procDir.resolve("images_final");
            if (!Files.isDirectory(imagesFinalDir)) {
                throw new IllegalStateException("Папка images_final/ не найдена в " + procDir);
            }

            /* === 4. сохраняем картинки и объекты в БД === */
            int processed = 0;
            for (DetectionJsonEntry entry : detections) {
                String fileName = entry.getFile();
                Path   image    = imagesFinalDir.resolve(fileName);
                if (!Files.exists(image)) {
                    log.warn("Файл '{}' отсутствует в images_final – пропускаем", fileName);
                    continue;
                }

                /* 4.1 грузим JPG в GridFS */
                String contentType = Optional
                        .ofNullable(Files.probeContentType(image))
                        .orElse("image/jpeg");
                String gridFsId;
                try (InputStream is = Files.newInputStream(image)) {
                    gridFsId = gridFsTemplate.store(is, fileName, contentType).toString();
                }

                /* 4.2 координаты */
                RawCoord rc = coordsMap.get(fileName);
                if (rc == null) {
                    log.warn("Координаты для '{}' не найдены – пропуск", fileName);
                    continue;
                }

                /* 4.3 сохраняем в БД */
                try {
                    dbSaver.save(image.toFile(), gridFsId,
                            batchName,
                            rc.getLatitude(), rc.getLongitude(),
                            entry.getClsName(), entry.getConf());
                    processed++;
                } catch (Exception ex) {
                    log.error("Ошибка DbSaver.save для '{}': ", fileName, ex);
                }
            }
            log.info("Обработка завершена, сохранено объектов: {}", processed);

        } finally {
            /* чистим tmp-директорию */
            FileUtils.deleteDirectory(tmpRoot.toFile());
            log.info("=== Завершена обработка партии '{}' ===", batchName);
        }
    }

    /* ---------- utils ---------- */

    private boolean isValidObjectId(String id) {
        try { new ObjectId(id); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    private void unzip(byte[] zipBytes, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    if (resolved.getParent() != null) Files.createDirectories(resolved.getParent());
                    try (OutputStream os = Files.newOutputStream(resolved)) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private Path findAnyCsvRecursively(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .findFirst().orElse(null);
        }
    }

    private Path findFileRecursively(Path root, String name) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }
    }

    private List<DetectionJsonEntry> parseDetectionsJson(Path json) throws IOException {
        try (InputStream is = Files.newInputStream(json)) {
            return objectMapper.readValue(is, new TypeReference<List<DetectionJsonEntry>>() {});
        }
    }

    private Map<String, RawCoord> parseCoordsCsv(Path csv) throws IOException {
        Map<String, RawCoord> map = new HashMap<>();
        try (BufferedReader r = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null) return map;

            String[] cols = header.split(",");
            int iImg = indexOf(cols, "imagename");
            int iLat = indexOf(cols, "latitude");
            int iLon = indexOf(cols, "longitude");
            int iHgt = indexOf(cols, "height");
            int iRol = indexOf(cols, "roll");
            int iPit = indexOf(cols, "pitch");
            int iHed = indexOf(cols, "heading");

            String line; int row = 0;
            while ((line = r.readLine()) != null) {
                row++;
                String[] p = line.split(",", -1);
                if (p.length <= Math.max(iImg, Math.max(iLat, iLon))) continue;

                String fn = p[iImg].trim();
                RawCoord rc = new RawCoord(
                        parseDoubleSafe(p[iLat]),
                        parseDoubleSafe(p[iLon]),
                        (iHgt >= 0 && p.length > iHgt) ? parseDoubleSafe(p[iHgt]) : 0,
                        (iRol >= 0 && p.length > iRol) ? parseDoubleSafe(p[iRol]) : 0,
                        (iPit >= 0 && p.length > iPit) ? parseDoubleSafe(p[iPit]) : 0,
                        (iHed >= 0 && p.length > iHed) ? parseDoubleSafe(p[iHed]) : 0
                );
                map.put(fn, rc);
            }
        }
        return map;
    }

    private int    indexOf(String[] arr, String name) { for (int i = 0; i < arr.length; i++) if (arr[i].trim().equalsIgnoreCase(name)) return i; return -1; }
    private String get(String[] a, int i) { return (i >= 0 && i < a.length) ? a[i] : ""; }
    private double parseDoubleSafe(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; } }

    /* ---------- вложенные классы ---------- */

    public static class DetectionJsonEntry {

        @JsonProperty("file")
        private String file;

        @JsonProperty("cls_name")
        private String clsName;

        @JsonProperty("conf")
        private double conf;

        @JsonProperty("x1") private int x1;
        @JsonProperty("y1") private int y1;
        @JsonProperty("x2") private int x2;
        @JsonProperty("y2") private int y2;

        @JsonProperty("side")
        private String side;

        @JsonProperty("shift")
        private double shift;

        /* геттеры — нужны лишь те, что реально используются */

        public String  getFile()    { return file; }
        public String  getClsName() { return clsName; }
        public double  getConf()    { return conf;  }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawCoord {
        private double latitude;
        private double longitude;
        private double height;
        private double roll;
        private double pitch;
        private double heading;
    }
}
