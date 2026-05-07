package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CustomerRequest;
import br.com.accenture.customer.api.dto.CustomerResponse;
import br.com.accenture.customer.api.mapper.CustomerDtoMapper;
import br.com.accenture.customer.application.service.CustomerService;
import br.com.accenture.customer.domain.model.Customer;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer created = customerService.create(CustomerDtoMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(CustomerDtoMapper.toResponse(created));
    }

    @GetMapping
    public Page<CustomerResponse> list(@RequestParam(required = false) String name,
                                       Pageable pageable) {
        Page<Customer> customers = (name == null || name.isBlank())
                ? customerService.findAll(pageable)
                : customerService.findByName(name, pageable);
        return customers.map(CustomerDtoMapper::toResponse);
    }

    @GetMapping("/{id}")
    public CustomerResponse findById(@PathVariable UUID id) {
        return CustomerDtoMapper.toResponse(customerService.findById(id));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody CustomerRequest request) {
        Customer updated = customerService.update(id, CustomerDtoMapper.toDomain(request));
        return CustomerDtoMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
