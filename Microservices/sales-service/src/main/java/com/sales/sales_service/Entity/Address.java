package com.sales.sales_service.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Customer customer;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AddressType addressType; // SHIPPING or BILLING

    @Column(nullable = false, length = 150)
    private String street;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 80)
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 60)
    private String country;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    public enum AddressType {
        SHIPPING,
        BILLING,
        BOTH
    }
}
