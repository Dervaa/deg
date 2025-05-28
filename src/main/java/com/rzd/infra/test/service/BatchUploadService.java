package com.rzd.infra.test.service;

import com.rzd.infra.test.util.ZipBatchProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BatchUploadService {

    private final ZipBatchProcessor processor;

    /** Принимает ZIP, бросает Exception наверх, чтобы контроллер ответил 400/500. */
    public void handle(MultipartFile zip) throws Exception {
        processor.process(zip.getBytes(), zip.getOriginalFilename());
    }
}
