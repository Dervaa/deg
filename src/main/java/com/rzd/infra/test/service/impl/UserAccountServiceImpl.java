package com.rzd.infra.test.service.impl;

import com.rzd.infra.test.entity.UserAccount;
import com.rzd.infra.test.entity.UserRole;
import com.rzd.infra.test.DTO.UserAccountDto;
import com.rzd.infra.test.repository.UserAccountRepository;
import com.rzd.infra.test.repository.UserRoleRepository;
import com.rzd.infra.test.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountServiceImpl implements UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;

    private UserAccountDto toDto(UserAccount entity) {
        return UserAccountDto.builder()
                .id(entity.getId())
                .roleId(entity.getRole().getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .middleName(entity.getMiddleName())
                .contacts(entity.getContacts())
                .build();
    }

    private UserAccount toEntity(UserAccountDto dto) {
        UserRole role = userRoleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + dto.getRoleId()));
        return UserAccount.builder()
                .role(role)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .middleName(dto.getMiddleName())
                .contacts(dto.getContacts())
                .build();
    }

    @Override
    public UserAccountDto create(UserAccountDto dto) {
        UserAccount saved = userAccountRepository.save(toEntity(dto));
        return toDto(saved);
    }

    @Override
    public UserAccountDto getById(Long id) {
        return userAccountRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Override
    public List<UserAccountDto> getAll() {
        return userAccountRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserAccountDto update(Long id, UserAccountDto dto) {
        UserAccount entity = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        UserRole role = userRoleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + dto.getRoleId()));
        entity.setRole(role);
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setMiddleName(dto.getMiddleName());
        entity.setContacts(dto.getContacts());
        return toDto(userAccountRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        userAccountRepository.deleteById(id);
    }
}
