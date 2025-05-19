package com.rzd.infra.test.config;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * Подключаем JtsModule, чтобы Jackson
     * автоматически конвертировал org.locationtech.jts.geom.Point
     * ↔ GeoJSON { type:"Point", coordinates:[lon,lat] }
     */
    @Bean
    public JtsModule jtsModule() {
        return new JtsModule();
    }
}
