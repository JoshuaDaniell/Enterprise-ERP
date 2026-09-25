package com.sales.sales_service.Dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String companyName;
    private String email;
    private String phone;
    private List<AddressDto> addresses;
    private Instant createdAt;
    private Instant updatedAt;
}
