package com.sales.sales_service.Config;

import com.sales.sales_service.Dto.AddressDto;
import com.sales.sales_service.Dto.CustomerRequestDto;
import com.sales.sales_service.Dto.CustomerResponseDto;
import com.sales.sales_service.Entity.Address;
import com.sales.sales_service.Repository.CustomerRepository;
import com.sales.sales_service.Repository.OrderRepository;
import com.sales.sales_service.Service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            log.info("Seeding initial ERP customer profiles...");

            CustomerRequestDto cust1 = CustomerRequestDto.builder()
                    .firstName("Alexander")
                    .lastName("Pierce")
                    .companyName("Stark Logistics Global")
                    .email("alexander.pierce@starklogistics.com")
                    .phone("+1-555-019-2834")
                    .addresses(List.of(
                            AddressDto.builder()
                                    .addressType(Address.AddressType.SHIPPING)
                                    .street("742 Evergreen Industrial Pkwy")
                                    .city("Chicago")
                                    .state("IL")
                                    .postalCode("60601")
                                    .country("USA")
                                    .isDefault(true)
                                    .build()
                    ))
                    .build();
            CustomerResponseDto savedCust1 = customerService.createCustomer(cust1);

            CustomerRequestDto cust2 = CustomerRequestDto.builder()
                    .firstName("Priya")
                    .lastName("Sharma")
                    .companyName("NovaTech Solutions")
                    .email("priya.sharma@novatech.io")
                    .phone("+91-98765-43210")
                    .addresses(List.of(
                            AddressDto.builder()
                                    .addressType(Address.AddressType.SHIPPING)
                                    .street("42 Cyber City, Sector 29")
                                    .city("Gurugram")
                                    .state("Haryana")
                                    .postalCode("122002")
                                    .country("India")
                                    .isDefault(true)
                                    .build()
                    ))
                    .build();
            customerService.createCustomer(cust2);

            log.info("Seeded default customers: '{}' and 'Priya Sharma'", savedCust1.getFullName());
        }
    }
}
