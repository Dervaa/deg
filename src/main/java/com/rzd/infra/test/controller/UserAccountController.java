package com.rzd.infra.test.controller;

import com.rzd.infra.test.DTO.UserAccountDto;
import com.rzd.infra.test.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserAccountController {
    private final UserAccountService userAccountService;

    @PostMapping
    public ResponseEntity<UserAccountDto> create(@Valid @RequestBody UserAccountDto dto) {
        UserAccountDto created = userAccountService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + created.getId()))
                .body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccountDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(userAccountService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserAccountDto>> getAll() {
        return ResponseEntity.ok(userAccountService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccountDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UserAccountDto dto) {
        return ResponseEntity.ok(userAccountService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAccountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

