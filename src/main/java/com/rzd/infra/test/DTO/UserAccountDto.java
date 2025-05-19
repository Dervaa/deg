package com.rzd.infra.test.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccountDto {
    private Long id;
    private Long roleId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String contacts;
}
