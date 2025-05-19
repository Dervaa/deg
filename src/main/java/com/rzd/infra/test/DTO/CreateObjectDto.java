package com.rzd.infra.test.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateObjectDto {
    @NotNull private Long typeId;
    @NotNull private Long statusId;
    @NotNull private Double latitude;
    @NotNull private Double longitude;
    private Double confidence;
}
