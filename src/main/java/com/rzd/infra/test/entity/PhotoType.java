package com.rzd.infra.test.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="object_type")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PhotoType {
    @Id
    @GeneratedValue
    Integer id;
    @Column(unique=true, nullable=false) String name;
}
