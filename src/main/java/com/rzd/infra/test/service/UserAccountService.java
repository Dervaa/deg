package com.rzd.infra.test.service;

import com.rzd.infra.test.DTO.UserAccountDto;
import java.util.List;

public interface UserAccountService {
    UserAccountDto create(UserAccountDto dto);
    UserAccountDto getById(Long id);
    List<UserAccountDto> getAll();
    UserAccountDto update(Long id, UserAccountDto dto);
    void delete(Long id);
}
