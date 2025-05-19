package com.rzd.infra.test.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "infrastructure_object")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InfrastructureObject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id")
    private ObjectType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(name = "photo_id", length = 64)
    private String photoId;

    /** Геометрия точки в WGS84 */
    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

    /** Уверенность модели в точности классификации */
    private Double confidence;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
