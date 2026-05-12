package br.com.accenture.payment.domain.payment.repository;

import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    PageResult<Payment> findAll(PageRequest pageRequest);

    void deleteById(UUID id);
}