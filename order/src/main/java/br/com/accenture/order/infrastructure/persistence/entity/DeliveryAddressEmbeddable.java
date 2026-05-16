package br.com.accenture.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryAddressEmbeddable {

    @Column(name = "delivery_street", length = 200)
    private String street;

    @Column(name = "delivery_number", length = 20)
    private String number;

    @Column(name = "delivery_complement", length = 100)
    private String complement;

    @Column(name = "delivery_neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "delivery_city", length = 100)
    private String city;

    @Column(name = "delivery_state", length = 2)
    private String state;

    @Column(name = "delivery_zip_code", length = 20)
    private String zipCode;
}
