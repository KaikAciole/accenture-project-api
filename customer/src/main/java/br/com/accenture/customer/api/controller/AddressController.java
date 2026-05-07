package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.AddressResponse;
import br.com.accenture.customer.api.mapper.AddressDtoMapper;
import br.com.accenture.customer.application.service.AddressService;
import br.com.accenture.customer.domain.model.Address;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@PathVariable UUID customerId,
                                                  @Valid @RequestBody AddressRequest request) {
        Address created = addressService.create(AddressDtoMapper.toDomain(request, customerId));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(AddressDtoMapper.toResponse(created));
    }

    @GetMapping
    public Page<AddressResponse> list(@PathVariable UUID customerId, Pageable pageable) {
        return addressService.findByCustomerId(customerId, pageable)
                .map(AddressDtoMapper::toResponse);
    }

    @GetMapping("/{addressId}")
    public AddressResponse findById(@PathVariable UUID customerId,
                                    @PathVariable UUID addressId) {
        return AddressDtoMapper.toResponse(addressService.findByCustomerAndId(customerId, addressId));
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(@PathVariable UUID customerId,
                                  @PathVariable UUID addressId,
                                  @Valid @RequestBody AddressRequest request) {
        Address updated = addressService.update(
                customerId,
                addressId,
                AddressDtoMapper.toDomain(request, customerId)
        );
        return AddressDtoMapper.toResponse(updated);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@PathVariable UUID customerId,
                                       @PathVariable UUID addressId) {
        addressService.delete(customerId, addressId);
        return ResponseEntity.noContent().build();
    }

}
