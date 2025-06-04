//package com.rzd.infra.test.util;
//
//import java.io.File;
//import java.util.concurrent.ThreadLocalRandom;
//
///** Заглушка «нейронки». */
//public final class StubDetector {
//
//    private StubDetector() {}
//
//    /** Результат «распознавания». */
//    public static class DetectionResult {
//        public final boolean hasObject;
//        public final String  objectType;
//        public final double  confidence;
//
//        private DetectionResult(boolean hasObject, String objectType, double confidence) {
//            this.hasObject   = hasObject;
//            this.objectType  = objectType;
//            this.confidence  = confidence;
//        }
//    }
//
//    /** 60 % снимков «с объектом», класс выбираем случайно. */
//    public static DetectionResult detect(File img) {
//        boolean ok = ThreadLocalRandom.current().nextDouble() < 0.60;
//        if (!ok) return new DetectionResult(false, null, 0.0);
//
//        String[] classes = {"Светофор", "Стрелочный перевод", "Опора контактной сети"};
//        String cls = classes[ThreadLocalRandom.current().nextInt(classes.length)];
//        double conf = 50 + ThreadLocalRandom.current().nextDouble() * 50;
//
//        return new DetectionResult(true, cls, conf);
//    }
//
//}
