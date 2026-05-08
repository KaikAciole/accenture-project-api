package br.com.accenture.inventory.application.service;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.exception.ProductNotFoundException;
import br.com.accenture.inventory.domain.exception.StockReservationNotFoundException;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.domain.repository.ProductRepository;
import br.com.accenture.inventory.domain.repository.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public PageResult<StockReservation> findAll(PageRequest pageRequest) {
        return stockReservationRepository.findAll(pageRequest);
    }

    @Transactional(readOnly = true)
    public PageResult<StockReservation> findByOrderId(UUID orderId, PageRequest pageRequest) {
        return stockReservationRepository.findByOrderId(orderId, pageRequest);
    }

    @Transactional(readOnly = true)
    public PageResult<StockReservation> findByOrderIdAndStatus(UUID orderId,
                                                               ReservationStatus status,
                                                               PageRequest pageRequest) {
        return stockReservationRepository.findByOrderIdAndStatus(orderId, status, pageRequest);
    }

    @Transactional(readOnly = true)
    public PageResult<StockReservation> findByProductId(UUID productId, PageRequest pageRequest) {
        return stockReservationRepository.findByProductId(productId, pageRequest);
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