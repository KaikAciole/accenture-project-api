package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.repository.StockReservationRepository;
import br.com.accenture.inventory.infrastructure.persistence.mapper.StockReservationPersistenceMapper;
import org.springframework.stereotype.Component;

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
    public List<StockReservation> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
                .map(StockReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status) {
        return jpaRepository.findByOrderIdAndStatus(orderId, status).stream()
                .map(StockReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StockReservation> findByProductId(UUID productId) {
        return jpaRepository.findByProductId(productId).stream()
                .map(StockReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<StockReservation> findAll() {
        return jpaRepository.findAll().stream()
                .map(StockReservationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}