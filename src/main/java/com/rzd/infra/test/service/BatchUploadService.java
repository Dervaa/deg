package com.rzd.infra.test.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.rzd.infra.test.entity.MongoBatch;
import com.rzd.infra.test.repository.MongoBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchUploadService {

    private final GridFsTemplate gridFsTemplate;
    private final MongoBatchRepository mongoBatchRepository;
    private final PythonClientService pythonClientService;
    private final MongoTemplate mongoTemplate;  // вместо MongoDatabaseFactory

    public void handleRawBatch(MultipartFile zipFile) throws Exception {
        String originalFilename = zipFile.getOriginalFilename();
        log.info("BatchUploadService.handleRawBatch: получен файл='{}'", originalFilename);

        // 1) Проверяем, что имя заканчивается на ".zip"
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            log.warn("Неверное расширение файла '{}', ожидается .zip", originalFilename);
            throw new IllegalArgumentException(
                    "Имя файла должно заканчиваться на \".zip\": " + originalFilename
            );
        }

        // 2) Извлекаем batchName без расширения
        String batchName = originalFilename.substring(0, originalFilename.length() - ".zip".length());
        // Разрешаем только цифры длины 8 или 14
        if (!batchName.matches("\\d{8}(\\d{6})?")) {
            log.warn("Неправильный формат batchName '{}' (только цифры, 8 или 14 символов)", batchName);
            throw new IllegalArgumentException(
                    "Некорректный формат batchName (только цифры, 8 или 14 символов): " + batchName
            );
        }

        // 3) Сохраняем ZIP в GridFS
        String filenameToStore = batchName + ".zip";
        ObjectId fileId;
        try (InputStream is = zipFile.getInputStream()) {
            log.debug("Размер входного файла '{}': {} байт", filenameToStore, zipFile.getSize());

            // 3.1) Сохраняем и сразу получаем ObjectId
            fileId = gridFsTemplate.store(is, filenameToStore, zipFile.getContentType());
            log.info("ZIP '{}' сохранён в GridFS, ObjectId = {}", filenameToStore, fileId);

            // 3.2) Проверяем, что GridFSFile найден по _id
            Query findByIdQuery = Query.query(Criteria.where("_id").is(fileId));
            GridFSFile gridFsFile = gridFsTemplate.findOne(findByIdQuery);
            if (gridFsFile == null) {
                log.error("GridFSFile для '{}' с ObjectId='{}' не найден после сохранения", filenameToStore, fileId);
                throw new RuntimeException("Файл не найден в GridFS после сохранения: " + fileId);
            }
            log.debug("GridFSFile найден по ObjectId='{}'.", fileId);

            // 3.3) Получаем ресурс (хотя его наличие уже подтверждено через findOne)
            GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
            if (resource == null || !resource.exists()) {
                log.error("GridFsResource для '{}' с ObjectId='{}' не существует", filenameToStore, fileId);
                throw new RuntimeException("GridFsResource отсутствует после сохранения: " + fileId);
            }
            log.debug("GridFsResource существует для '{}' с ObjectId='{}'.", filenameToStore, fileId);

            // 3.4) Проверяем количество чанков, используя реальный chunkSize из gridFsFile
            int realChunkSize = gridFsFile.getChunkSize(); // обычно ~255KiB, если не переопределяли
            long expectedChunks = (zipFile.getSize() + realChunkSize - 1) / realChunkSize;

            // Получаем MongoDatabase через mongoTemplate
            MongoDatabase db = mongoTemplate.getDb();
            MongoCollection<Document> chunksCollection = db.getCollection("fs.chunks");
            long actualChunkCount = chunksCollection.countDocuments(
                    new Document("files_id", fileId)
            );

            log.info(
                    "Для файла '{}' (ObjectId={}) – найдено чанков={}, ожидалось ~{} (chunkSize={})",
                    filenameToStore, fileId, actualChunkCount, expectedChunks, realChunkSize
            );
            if (actualChunkCount < expectedChunks) {
                log.error(
                        "Недостаточно чанков для '{}': {} из ~{} (chunkSize={})",
                        filenameToStore, actualChunkCount, expectedChunks, realChunkSize
                );
                throw new RuntimeException(
                        String.format("Недостаточно чанков: %d из ~%d (chunkSize=%d)",
                                actualChunkCount, expectedChunks, realChunkSize)
                );
            }
        } catch (Exception e) {
            log.error("Ошибка при сохранении '{}' в GridFS: ", filenameToStore, e);
            throw new RuntimeException("Ошибка при сохранении raw ZIP в GridFS: " + e.getMessage(), e);
        }

        // 4) Создаём или обновляем MongoBatch, сохраняя строковую версию ObjectId
        MongoBatch mb = mongoBatchRepository.findByBatchName(batchName)
                .orElseGet(() -> {
                    log.info("Создаётся новый документ MongoBatch для batchName='{}'.", batchName);
                    return MongoBatch.builder().batchName(batchName).build();
                });

        mb.setRawZipId(fileId.toString());
        mb.setProcessedZipId(null); // сбросим на случай, если раньше был ненулевой
        mongoBatchRepository.save(mb);
        log.info("Документ MongoBatch (batchName='{}') сохранён/обновлён.", batchName);

        // 5) Отправляем ZIP Python-воркеру
        pythonClientService.sendZipToPython(zipFile);
        log.info("Raw ZIP '{}' передан на асинхронную обработку Python-воркеру.", originalFilename);
    }
}
