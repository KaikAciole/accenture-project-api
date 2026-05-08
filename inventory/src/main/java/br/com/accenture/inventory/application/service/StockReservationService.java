package br.com.accenture.inventory.application.service;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.exception.ProductNotFoundException;
import br.com.accenture.inventory.domain.exception.StockReservationNotFoundException;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.repository.ProductRepository;
import br.com.accenture.inventory.domain.repository.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StockReservationService {

    private final StockReservationRepository stockReservationRepository;
    private final ProductRepository productRepository;

    public StockReservationService(StockReservationRepository stockReservationRepository,
                                   ProductRepository productRepository) {
        this.stockReservationRepository = stockReservationRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public StockReservation create(UUID orderId, UUID productId, Integer reservedQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        StockReservation reservation = StockReservation.createNew(
                orderId,
                product,
                reservedQuantity
        );

        productRepository.save(product);

        return stockReservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public StockReservation findById(UUID id) {
        return stockReservationRepository.findById(id)
                .orElseThrow(() -> new StockReservationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<StockReservation> findAll() {
        return stockReservationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<StockReservation> findByOrderId(UUID orderId) {
        return stockReservationRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status) {
        return stockReservationRepository.findByOrderIdAndStatus(orderId, status);
    }

    @Transactional(readOnly = true)
    public List<StockReservation> findByProductId(UUID productId) {
        return stockReservationRepository.findByProductId(productId);
    }

    @Transactional
    public StockReservation confirm(UUID id) {
        StockReservation reservation = stockReservationRepository.findById(id)
                .orElseThrow(() -> new StockReservationNotFoundException(id));

        reservation.confirm();

        return stockReservationRepository.save(reservation);
    }

    @Transactional
    public StockReservation cancel(UUID id) {
        StockReservation reservation = stockReservationRepository.findById(id)
                .orElseThrow(() -> new StockReservationNotFoundException(id));

        reservation.cancel();

        productRepository.save(reservation.getProduct());

        return stockReservationRepository.save(reservation);
    }

    @Transactional
    public StockReservation expire(UUID id) {
        StockReservation reservation = stockReservationRepository.findById(id)
                .orElseThrow(() -> new StockReservationNotFoundException(id));

        reservation.expire();

        productRepository.save(reservation.getProduct());

        return stockReservationRepository.save(reservation);
    }

    @Transactional
    public void delete(UUID id) {
        if (stockReservationRepository.findById(id).isEmpty()) {
            throw new StockReservationNotFoundException(id);
        }

        stockReservationRepository.deleteById(id);
    }
}