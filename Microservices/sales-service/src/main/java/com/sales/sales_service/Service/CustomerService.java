package com.sales.sales_service.Service;

import com.sales.sales_service.Dto.AddressDto;
import com.sales.sales_service.Dto.CustomerRequestDto;
import com.sales.sales_service.Dto.CustomerResponseDto;
import com.sales.sales_service.Entity.Address;
import com.sales.sales_service.Entity.Customer;
import com.sales.sales_service.Exception.ResourceNotFoundException;
import com.sales.sales_service.Repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.sales.sales_service.Config.RedisConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisConfig.CUSTOMERS)
    public List<CustomerResponseDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return mapToResponseDto(customer);
    }

    @Transactional
    @CacheEvict(cacheNames = RedisConfig.CUSTOMERS, allEntries = true)
    public CustomerResponseDto createCustomer(CustomerRequestDto dto) {
        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Customer with email '" + dto.getEmail() + "' already exists");
        }

        Customer customer = Customer.builder()
                .firstName(dto.getFirstName().trim())
                .lastName(dto.getLastName().trim())
                .companyName(dto.getCompanyName() != null ? dto.getCompanyName().trim() : null)
                .email(dto.getEmail().trim().toLowerCase())
                .phone(dto.getPhone() != null ? dto.getPhone().trim() : null)
                .build();

        if (dto.getAddresses() != null) {
            for (AddressDto addrDto : dto.getAddresses()) {
                Address address = Address.builder()
                        .addressType(addrDto.getAddressType() != null ? addrDto.getAddressType() : Address.AddressType.BOTH)
                        .street(addrDto.getStreet())
                        .city(addrDto.getCity())
                        .state(addrDto.getState())
                        .postalCode(addrDto.getPostalCode())
                        .country(addrDto.getCountry())
                        .isDefault(addrDto.isDefault())
                        .build();
                customer.addAddress(address);
            }
        }

        Customer saved = customerRepository.save(customer);
        log.info("Created new customer '{}' with ID {}", saved.getFullName(), saved.getId());
        return mapToResponseDto(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = RedisConfig.CUSTOMERS, allEntries = true)
    public CustomerResponseDto updateCustomer(Long id, CustomerRequestDto dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        customer.setFirstName(dto.getFirstName().trim());
        customer.setLastName(dto.getLastName().trim());
        customer.setCompanyName(dto.getCompanyName() != null ? dto.getCompanyName().trim() : null);
        customer.setEmail(dto.getEmail().trim().toLowerCase());
        customer.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);

        Customer saved = customerRepository.save(customer);
        return mapToResponseDto(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = RedisConfig.CUSTOMERS, allEntries = true)
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customerRepository.delete(customer);
        log.info("Deleted customer with ID {}", id);
    }

    public CustomerResponseDto mapToResponseDto(Customer customer) {
        List<AddressDto> addressDtos = customer.getAddresses() != null
                ? customer.getAddresses().stream().map(a -> AddressDto.builder()
                        .id(a.getId())
                        .addressType(a.getAddressType())
                        .street(a.getStreet())
                        .city(a.getCity())
                        .state(a.getState())
                        .postalCode(a.getPostalCode())
                        .country(a.getCountry())
                        .isDefault(a.isDefault())
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        return CustomerResponseDto.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .fullName(customer.getFullName())
                .companyName(customer.getCompanyName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .addresses(addressDtos)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
