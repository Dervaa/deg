package com.rzd.infra.test.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.*;
import com.drew.metadata.jpeg.JpegDirectory;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Утилита-читалка EXIF. */
public final class ExifUtil {

    private ExifUtil() {}

    /** Данные, которые выдёргиваем из EXIF. */
    public static class ExifData {
        public final Double lat;
        public final Double lon;
        public final Instant shotTime;
        public final Integer width;
        public final Integer height;

        private ExifData(Double lat, Double lon, Instant shotTime,
                         Integer width, Integer height) {
            this.lat = lat;
            this.lon = lon;
            this.shotTime = shotTime;
            this.width = width;
            this.height = height;
        }
    }

    public static ExifData read(File f) {
        try {
            Metadata m = ImageMetadataReader.readMetadata(f);

            GpsDirectory gps = m.getFirstDirectoryOfType(GpsDirectory.class);
            Double lat = gps != null && gps.getGeoLocation() != null
                    ? gps.getGeoLocation().getLatitude()  : null;
            Double lon = gps != null && gps.getGeoLocation() != null
                    ? gps.getGeoLocation().getLongitude() : null;

            ExifSubIFDDirectory sub = m.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Instant shot = Optional.ofNullable(sub)
                    .map(ExifSubIFDDirectory::getDateOriginal)
                    .map(Date::toInstant)
                    .orElse(null);

            JpegDirectory jpg = m.getFirstDirectoryOfType(JpegDirectory.class);
            Integer w = jpg != null ? jpg.getImageWidth()  : null;
            Integer h = jpg != null ? jpg.getImageHeight() : null;

            return new ExifData(lat, lon, shot, w, h);
        } catch (Exception e) {            // битое фото
            return null;
        }
    }
}
