package com.rzd.infra.test.util;

import com.rzd.infra.test.util.StubDetector.DetectionResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
public class ZipBatchProcessor {

    private final GridFsTemplate gridFs;
    private final DbSaver dbSaver;

    /**
     * Разбирает ZIP-архив:
     * 1) распаковывает в temp/raw;
     * 2) для каждого изображения:
     *    – детектирует StubDetector.detect(img);
     *      • если объекта нет – удаляет файл;
     *      • если есть – заливает в GridFS, читает EXIF, сохраняет в БД и перемещает в temp/ok;
     * 3) удаляет raw-папку.
     *
     * @param zipBytes байты ZIP-файла
     * @param zipName  имя файла, например "20250525_143020_photos.zip"
     */
    public void process(byte[] zipBytes, String zipName) throws IOException {
        String batchName = stripExtension(zipName);

        Path tempDir = Files.createTempDirectory("batch_");
        Path rawDir  = tempDir.resolve("raw");
        Path okDir   = tempDir.resolve("ok");
        Files.createDirectories(rawDir);
        Files.createDirectories(okDir);

        unzip(zipBytes, rawDir);

        List<File> images = FileUtils.listFiles(rawDir.toFile(),
                new String[] { "jpg", "jpeg", "png" }, true).stream().toList();

        for (File img : images) {
            DetectionResult dr = StubDetector.detect(img);

            if (!dr.hasObject) {
                // нет объекта – удаляем
                img.delete();
                continue;
            }

            // заливаем оригинал в GridFS
            String gridId;
            try (InputStream is = new FileInputStream(img)) {
                gridId = gridFs.store(
                        is,
                        img.getName(),
                        Files.probeContentType(img.toPath())
                ).toString();
            }

            // читаем EXIF
            ExifUtil.ExifData ex = ExifUtil.read(img);

            // сохраняем в БД: Photo + InfrastructureObject
            dbSaver.save(
                    img,
                    gridId,
                    batchName,
                    ex,
                    dr.objectType,
                    dr.confidence
            );

            // перемещаем в ok/
            Files.move(img.toPath(),
                    okDir.resolve(img.getName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        // удаляем raw/
        FileUtils.deleteDirectory(rawDir.toFile());
    }

    private static void unzip(byte[] zipBytes, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (OutputStream os = Files.newOutputStream(outPath)) {
                        zis.transferTo(os);
                    }
                }
            }
        }
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }
}
