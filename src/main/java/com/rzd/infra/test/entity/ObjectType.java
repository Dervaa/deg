package com.rzd.infra.test.entity;


import jakarta.persistence.*;
import lombok.*;

/** Справочник категорий объектов инфраструктуры.*/
@Entity
@Table(name = "object_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}