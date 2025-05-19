package com.rzd.infra.test.config;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoConfig {
    @Bean
    public GeometryFactory geometryFactory() {
        // 4326 — WGS84
        return new GeometryFactory(new PrecisionModel(), 4326);
    }
}
