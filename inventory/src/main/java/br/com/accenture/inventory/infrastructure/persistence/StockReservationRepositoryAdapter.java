package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.domain.repository.StockReservationRepository;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import br.com.accenture.inventory.infrastructure.persistence.mapper.PageableMapper;
import br.com.accenture.inventory.infrastructure.persistence.mapper.StockReservationPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockReservationRepositoryAdapter implements StockReservationRepository {

    private final StockReservationJpaRepository jpaRepository;

    public StockReservationRepositoryAdapter(StockReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockReservation save(StockReservation stockReservation) {
        var entity = StockReservationPersistenceMapper.toEntity(stockReservation);
        var saved = jpaRepository.save(entity);
        return StockReservationPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<StockReservation> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(StockReservationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<StockReservation> findByOrderIdAndProductId(UUID orderId, UUID productId) {
        return jpaRepository.findByOrderIdAndProduct_Id(orderId, productId)
                .map(StockReservationPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<StockReservation> findByOrderId(UUID orderId, PageRequest pageRequest) {
        Page<StockReservationJpaEntity> page = jpaRepository.findByOrderId(
                orderId,
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public PageResult<StockReservation> findByOrderIdAndStatus(
            UUID orderId,
            ReservationStatus status,
            PageRequest pageRequest
    ) {
        Page<StockReservationJpaEntity> page = jpaRepository.findByOrderIdAndStatus(
                orderId,
                status,
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public PageResult<StockReservation> findByProductId(UUID productId, PageRequest pageRequest) {
        Page<StockReservationJpaEntity> page = jpaRepository.findByProductId(
                productId,
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public PageResult<StockReservation> findAll(PageRequest pageRequest) {
        Page<StockReservationJpaEntity> page = jpaRepository.findAll(
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public List<StockReservation> findAllByOrderIdAndStatus(UUID orderId, ReservationStatus status) {
        return jpaRepository.findAllByOrderIdAndStatus(orderId, status)
                .stream()
                .map(StockReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StockReservation> findAllByOrderIdAndStatusIn(UUID orderId, Collection<ReservationStatus> statuses) {
        return jpaRepository.findAllByOrderIdAndStatusIn(orderId, statuses)
                .stream()
                .map(StockReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private PageResult<StockReservation> toPageResult(Page<StockReservationJpaEntity> page) {
        return new PageResult<>(
                page.getContent().stream()
                        .map(StockReservationPersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
