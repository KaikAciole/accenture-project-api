package br.com.accenture.payment.infrastructure.persistence;

import br.com.accenture.payment.domain.model.Payment;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.repository.PaymentRepository;
import br.com.accenture.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import br.com.accenture.payment.infrastructure.persistence.mapper.PageableMapper;
import br.com.accenture.payment.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import org.springframework.data.domain.Page;
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
    public PageResult<Payment> findAll(PageRequest pageRequest) {
        Page<PaymentJpaEntity> page = jpaRepository.findAll(
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private PageResult<Payment> toPageResult(Page<PaymentJpaEntity> page) {
        return new PageResult<>(
                page.getContent().stream()
                        .map(PaymentPersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}