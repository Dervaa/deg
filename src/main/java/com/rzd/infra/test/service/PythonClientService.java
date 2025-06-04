package com.rzd.infra.test.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonClientService {

    @Value("${python.worker.url}")
    private String pythonWorkerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Отправляет ZIP (multipart/form-data) на внешний Python-воркер.
     * Бросает RuntimeException, если статус != 200.
     */
    @Async
    public void sendZipToPython(MultipartFile zipFile) {
        String originalFilename = zipFile.getOriginalFilename();
        log.debug("PythonClientService.sendZipToPython → попытка отправки '{}' на {}", originalFilename, pythonWorkerUrl);

        try {
            // 1) Формируем тело запроса: multipart/form-data с одним полем "file"
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource contentsAsResource = new ByteArrayResource(zipFile.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };
            body.add("file", contentsAsResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 2) Отправляем синхронный POST-запрос
            log.debug("RestTemplate POST → URL='{}', filename='{}'", pythonWorkerUrl, originalFilename);
            ResponseEntity<String> response = restTemplate.exchange(
                    pythonWorkerUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            HttpStatus statusCode = (HttpStatus) response.getStatusCode();
            String bodyResp = response.getBody();
            log.debug("Получен ответ от Python-воркера: status={}, body='{}'", statusCode, bodyResp);

            if (!statusCode.is2xxSuccessful()) {
                log.error("Python worker вернул ненулевой статус: {}", statusCode);
                throw new RuntimeException("Python worker вернул статус: " + statusCode);
            }

            log.info("PythonClientService: '{}' успешно доставлен воркеру (статус {})", originalFilename, statusCode);

        } catch (Exception e) {
            log.error("Ошибка при отправке '{}' на Python-воркер: {}", originalFilename, e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке ZIP на Python: " + e.getMessage(), e);
        }
    }

    /**
     * Если понадобится залогировать URL, можно вернуть это свойство
     */
    public String getPythonWorkerUrl() {
        return pythonWorkerUrl;
    }
}
