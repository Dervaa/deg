package com.rzd.infra.test.service;

import com.rzd.infra.test.entity.MongoBatch;
import com.rzd.infra.test.repository.MongoBatchRepository;
import com.rzd.infra.test.util.ProcessedBatchProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonCallbackService {

    private final GridFsTemplate gridFsTemplate;
    private final MongoBatchRepository mongoBatchRepository;
    private final ProcessedBatchProcessor processedBatchProcessor;

    /**
     * 1) Сохраняем «обработанный» ZIP в GridFS → получаем processedZipId
     * 2) Обновляем документ MongoBatch (уже по batchName) → устанавливаем processedZipId
     * 3) Сразу вызываем ProcessedBatchProcessor: передаём batchName, rawZipId и байты processed ZIP
     */
    public void handleProcessedBatch(MultipartFile zipFile) throws Exception {
        String originalFilename = zipFile.getOriginalFilename();
        log.info("PythonCallbackService.handleProcessedBatch: получен файл='{}'", originalFilename);

        // --- 1. Извлекаем batchName из имени файла вида "<batchName>_processed.zip" ---
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith("_processed.zip")) {
            log.warn("Некорректное имя обработанного архива '{}'", originalFilename);
            throw new IllegalArgumentException("Неправильное имя файла. Ожидается <batchName>_processed.zip, получили: " + originalFilename);
        }
        String batchName = originalFilename.substring(0, originalFilename.length() - "_processed.zip".length());
        if (!batchName.matches("\\d{8,14}")) {
            log.warn("Некорректный формат batchName '{}' извлечён из '{}'", batchName, originalFilename);
            throw new IllegalArgumentException("Некорректный формат batchName (timestamp): " + batchName);
        }
        log.debug("Извлечён batchName='{}' из файла '{}'", batchName, originalFilename);

        // --- 2. Сохраняем processed ZIP в GridFS ---
        String filenameToStore = batchName + "_processed.zip";
        log.debug("Попытка сохранить processed ZIP '{}' в GridFS под именем '{}'", originalFilename, filenameToStore);
        String processedZipId;
        try (InputStream is = zipFile.getInputStream()) {
            processedZipId = gridFsTemplate.store(is, filenameToStore, zipFile.getContentType()).toString();
            log.info("Processed ZIP '{}' сохранён в GridFS с ID='{}'", filenameToStore, processedZipId);
        } catch (Exception e) {
            log.error("Ошибка при сохранении processed ZIP '{}' в GridFS: ", originalFilename, e);
            throw e;
        }

        // --- 3. Находим существующий документ MongoBatch по batchName ---
        log.debug("Ищем документ MongoBatch по batchName='{}'", batchName);
        MongoBatch mb = mongoBatchRepository.findByBatchName(batchName)
                .orElseThrow(() -> {
                    log.error("MongoBatch с batchName='{}' не найден", batchName);
                    return new IllegalStateException("Batch не найден в Mongo: " + batchName);
                });

        // Устанавливаем processedZipId и сохраняем
        mb.setProcessedZipId(processedZipId);
        mongoBatchRepository.save(mb);
        log.info("MongoBatch '{}' обновлен: установлен processedZipId='{}'", batchName, processedZipId);

        // --- 4. Вызываем ProcessedBatchProcessor: передаём batchName, rawZipId и processedZipBytes ---
        String rawZipId = mb.getRawZipId();
        log.debug("rawZipId для '{}' = '{}'", batchName, rawZipId);
        byte[] processedZipBytes = zipFile.getBytes();
        log.debug("Длина массива processedZipBytes для '{}': {}", batchName, processedZipBytes.length);

        log.info("Запускаем ProcessedBatchProcessor.processBatch для batchName='{}'", batchName);
        try {
            processedBatchProcessor.processBatch(batchName, rawZipId, processedZipBytes);
            log.info("ProcessedBatchProcessor.processBatch для '{}' выполнен успешно", batchName);
        } catch (Exception e) {
            log.error("Ошибка при выполнении ProcessedBatchProcessor для '{}': ", batchName, e);
            throw e;
        }

        // (Опционально: здесь можно из «сырного» rawZip тоже достать csvCoordinates
        //  и сохранить mb.setCsvCoordinates(…) → mongoBatchRepository.save(mb))
    }
}
