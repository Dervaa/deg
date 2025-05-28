package com.rzd.infra.test.entity;

import lombok.*;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "photo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private PhotoType type;

    /** где снято, заполним из EXIF */
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geom;

    /** путь или URL до файла */
    @Column(nullable = false, length = 512)
    private String path;

    /** имя пачки (zip-архива) */
    @Column(name = "batch_name", length = 255)
    private String batchName;

    /** время снимка из EXIF */
    private Instant shotTime;

    private String source;
    private String resolution;
    private Double confidence;

    /** Привязка к инфра-объекту */
    @ManyToOne
    @JoinColumn(name = "object_id")
    private InfrastructureObject object;
}
