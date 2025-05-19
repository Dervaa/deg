package com.rzd.infra.test.controller;

import com.rzd.infra.test.DTO.CreateObjectDto;
import com.rzd.infra.test.entity.InfrastructureObject;
import com.rzd.infra.test.entity.ObjectType;
import com.rzd.infra.test.entity.Status;
import com.rzd.infra.test.repository.ObjectTypeRepository;
import com.rzd.infra.test.repository.StatusRepository;
import com.rzd.infra.test.service.InfrastructureObjectService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/objects")
@Validated
public class InfrastructureObjectController {

    private final InfrastructureObjectService svc;
    private final ObjectTypeRepository typeRepo;
    private final StatusRepository statusRepo;
    private final GeometryFactory gf;

    public InfrastructureObjectController(InfrastructureObjectService svc,
                                          ObjectTypeRepository typeRepo,
                                          StatusRepository statusRepo,
                                          GeometryFactory geometryFactory) {
        this.svc = svc;
        this.typeRepo = typeRepo;
        this.statusRepo = statusRepo;
        this.gf = geometryFactory;
    }

    @GetMapping
    public List<InfrastructureObject> list() {
        return svc.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InfrastructureObject> get(@PathVariable Long id) {
        return svc.get(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // —————————————————————————————————————————
    // Этот метод заменяет прежний @PostMapping(@RequestBody InfrastructureObject)
    @PostMapping
    public ResponseEntity<InfrastructureObject> create(
            @Valid @RequestBody CreateObjectDto dto) {

        // 1) находим объекты справочников
        ObjectType type = typeRepo.findById(dto.getTypeId())
                .orElseThrow(() -> new IllegalArgumentException("typeId not found"));
        Status status = statusRepo.findById(dto.getStatusId())
                .orElseThrow(() -> new IllegalArgumentException("statusId not found"));

        // 2) собираем JTS Point из простых координат
        Point p = gf.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));

        // 3) билдим сущность и сохраняем
        InfrastructureObject obj = InfrastructureObject.builder()
                .type(type)
                .status(status)
                .geom(p)
                .confidence(dto.getConfidence())
                .build();

        InfrastructureObject saved = svc.create(obj);
        return ResponseEntity.ok(saved);
    }
    // —————————————————————————————————————————

    @PutMapping("/{id}")
    public ResponseEntity<InfrastructureObject> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateObjectDto dto) {

        // Аналогично: можно повторно собирать Point и менять тип/статус
        InfrastructureObject existing = svc.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Object not found"));

        ObjectType type = typeRepo.findById(dto.getTypeId())
                .orElseThrow(() -> new IllegalArgumentException("typeId not found"));
        Status status = statusRepo.findById(dto.getStatusId())
                .orElseThrow(() -> new IllegalArgumentException("statusId not found"));

        Point p = gf.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));

        existing.setType(type);
        existing.setStatus(status);
        existing.setGeom(p);
        existing.setConfidence(dto.getConfidence());

        return ResponseEntity.ok(svc.update(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }
}
