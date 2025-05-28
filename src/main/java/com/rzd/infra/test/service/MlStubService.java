package com.rzd.infra.test.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class MlStubService {

    private static final String[] CLASSES = {
            "Светофор", "Стрелочный перевод", "Опора контактной сети"
    };
    private final Random rnd = new Random();

    /* key = файл, value = null если объекта нет  */
    Map<File, Result> detect(List<File> files) {
        try { Thread.sleep(2000 + rnd.nextInt(1000)); } catch (InterruptedException ignored) {}

        Map<File, Result> out = new HashMap<>();
        for (File f : files) {
            if (rnd.nextDouble() < 0.6) {          // 60 % позитив
                double conf = 50 + rnd.nextDouble() * 50;
                String cls  = CLASSES[rnd.nextInt(CLASSES.length)];
                out.put(f, new Result(cls, conf));
            } else {
                out.put(f, null);                  // негатив
            }
        }
        return out;
    }

    /** Простейший контейнер (static-inner), не DTO-файл. */
    public static class Result {
        final String objectType;
        final double confidence;
        Result(String t, double c) { objectType = t; confidence = c; }
    }
}

