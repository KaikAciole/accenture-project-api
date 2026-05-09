package br.com.accenture.payment.infrastructure.persistence;

import br.com.accenture.payment.domain.model.Payment;
import br.com.accenture.payment.domain.repository.PaymentRepository;
import br.com.accenture.payment.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        var entity = PaymentPersistenceMapper.toEntity(payment);
        var saved = jpaRepository.save(entity);
        return PaymentPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(PaymentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId)
                .map(PaymentPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpaRepository.existsByOrderId(orderId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
