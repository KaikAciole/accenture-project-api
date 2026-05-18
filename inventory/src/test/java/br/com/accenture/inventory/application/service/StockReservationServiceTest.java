package br.com.accenture.inventory.application.service;

import br.com.accenture.inventory.application.port.StockEventPublisher;
import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.exception.ProductNotFoundException;
import br.com.accenture.inventory.domain.exception.StockReservationNotFoundException;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.domain.repository.ProductRepository;
import br.com.accenture.inventory.domain.repository.StockReservationRepository;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StockReservationServiceTest {

    private final StockReservationRepository reservationRepository = mock(StockReservationRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StockEventPublisher stockEventPublisher = mock(StockEventPublisher.class);
    private final StockReservationService service = new StockReservationService(
            reservationRepository,
            productRepository,
            stockEventPublisher
    );

    @Test
    void createReservesStockAndPersistsProductAndReservation() {
        Product product = TestFixtures.restoredProduct();
        StockReservation saved = TestFixtures.activeReservation();
        when(productRepository.findById(TestFixtures.PRODUCT_ID)).thenReturn(Optional.of(product));
        when(reservationRepository.findByOrderIdAndProductId(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID))
                .thenReturn(Optional.empty());
        when(reservationRepository.save(any(StockReservation.class))).thenReturn(saved);

        StockReservation result = service.create(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID, 3);

        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(result).isSameAs(saved);
        verify(productRepository).save(product);
        verify(reservationRepository).save(any(StockReservation.class));
        verify(stockEventPublisher).publishStockReserved(saved);
    }

    @Test
    void createBySkuReservesStockAndPublishesReservedEvent() {
        Product product = TestFixtures.restoredProduct();
        StockReservation saved = TestFixtures.activeReservation();
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));
        when(reservationRepository.findByOrderIdAndProductId(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID))
                .thenReturn(Optional.empty());
        when(reservationRepository.save(any(StockReservation.class))).thenReturn(saved);

        StockReservation result = service.createBySku(TestFixtures.ORDER_ID, "SKU-001", 3);

        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(result).isSameAs(saved);
        verify(productRepository).save(product);
        verify(reservationRepository).save(any(StockReservation.class));
        verify(stockEventPublisher).publishStockReserved(saved);
    }

    @Test
    void createBySkuReturnsExistingReservationWithoutReservingStockAgain() {
        Product product = TestFixtures.restoredProduct();
        StockReservation existing = TestFixtures.activeReservation();
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));
        when(reservationRepository.findByOrderIdAndProductId(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID))
                .thenReturn(Optional.of(existing));

        StockReservation result = service.createBySku(TestFixtures.ORDER_ID, "SKU-001", 3);

        assertThat(result).isSameAs(existing);
        assertThat(product.getStockQuantity()).isEqualTo(10);
        verify(reservationRepository, never()).save(any(StockReservation.class));
        verify(productRepository, never()).save(product);
        verifyNoInteractions(stockEventPublisher);
    }

    @Test
    void createThrowsWhenProductIsMissing() {
        when(productRepository.findById(TestFixtures.PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ProductNotFoundException.class)
                .isThrownBy(() -> service.create(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID, 3))
                .withMessage("Product not found with id: " + TestFixtures.PRODUCT_ID);
        verifyNoInteractions(reservationRepository);
        verifyNoInteractions(stockEventPublisher);
    }

    @Test
    void createBySkuThrowsWhenProductIsMissing() {
        when(productRepository.findBySku("SKU-404")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ProductNotFoundException.class)
                .isThrownBy(() -> service.createBySku(TestFixtures.ORDER_ID, "SKU-404", 3))
                .withMessage("Product not found with sku: SKU-404");
        verifyNoInteractions(reservationRepository);
        verifyNoInteractions(stockEventPublisher);
    }

    @Test
    void findDelegatesQueriesAndThrowsWhenMissing() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        StockReservation reservation = TestFixtures.activeReservation();
        PageResult<StockReservation> page = new PageResult<>(List.of(reservation), 0, 10, 1, 1);
        when(reservationRepository.findById(TestFixtures.RESERVATION_ID)).thenReturn(Optional.of(reservation), Optional.empty());
        when(reservationRepository.findAll(pageRequest)).thenReturn(page);
        when(reservationRepository.findByOrderId(TestFixtures.ORDER_ID, pageRequest)).thenReturn(page);
        when(reservationRepository.findByOrderIdAndStatus(TestFixtures.ORDER_ID, ReservationStatus.ACTIVE, pageRequest)).thenReturn(page);
        when(reservationRepository.findByProductId(TestFixtures.PRODUCT_ID, pageRequest)).thenReturn(page);

        assertThat(service.findById(TestFixtures.RESERVATION_ID)).isSameAs(reservation);
        assertThat(service.findAll(pageRequest)).isSameAs(page);
        assertThat(service.findByOrderId(TestFixtures.ORDER_ID, pageRequest)).isSameAs(page);
        assertThat(service.findByOrderIdAndStatus(TestFixtures.ORDER_ID, ReservationStatus.ACTIVE, pageRequest)).isSameAs(page);
        assertThat(service.findByProductId(TestFixtures.PRODUCT_ID, pageRequest)).isSameAs(page);
        assertThatExceptionOfType(StockReservationNotFoundException.class)
                .isThrownBy(() -> service.findById(TestFixtures.RESERVATION_ID))
                .withMessage("Stock reservation not found with id: " + TestFixtures.RESERVATION_ID);
    }

    @Test
    void confirmCancelAndExpireApplyStatusChanges() {
        StockReservation toConfirm = TestFixtures.activeReservation();
        StockReservation toCancel = TestFixtures.activeReservation();
        StockReservation toExpire = TestFixtures.activeReservation();
        when(reservationRepository.findById(TestFixtures.RESERVATION_ID))
                .thenReturn(Optional.of(toConfirm), Optional.of(toCancel), Optional.of(toExpire));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockReservation confirmed = service.confirm(TestFixtures.RESERVATION_ID);
        StockReservation canceled = service.cancel(TestFixtures.RESERVATION_ID);
        StockReservation expired = service.expire(TestFixtures.RESERVATION_ID);

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(canceled.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(canceled.getProduct().getStockQuantity()).isEqualTo(13);
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expired.getProduct().getStockQuantity()).isEqualTo(13);
        verify(productRepository).save(canceled.getProduct());
        verify(productRepository).save(expired.getProduct());
        verify(stockEventPublisher).publishStockReservationConfirmed(confirmed);
        verify(stockEventPublisher).publishStockReservationCanceled(canceled);
    }

    @Test
    void confirmByOrderIdConfirmsActiveReservationsAndPublishesEvents() {
        StockReservation first = TestFixtures.activeReservation();
        StockReservation second = TestFixtures.activeReservation();
        when(reservationRepository.findAllByOrderIdAndStatus(TestFixtures.ORDER_ID, ReservationStatus.ACTIVE))
                .thenReturn(List.of(first, second));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmByOrderId(TestFixtures.ORDER_ID);

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(stockEventPublisher).publishStockReservationConfirmed(first);
        verify(stockEventPublisher).publishStockReservationConfirmed(second);
    }

    @Test
    void cancelByOrderIdCancelsActiveReservationsAndPublishesEvents() {
        StockReservation first = TestFixtures.activeReservation();
        StockReservation second = TestFixtures.activeReservation();
        when(reservationRepository.findAllByOrderIdAndStatusIn(
                TestFixtures.ORDER_ID,
                List.of(ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED)
        )).thenReturn(List.of(first, second));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelByOrderId(TestFixtures.ORDER_ID);

        assertThat(first.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(second.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        verify(productRepository).save(first.getProduct());
        verify(productRepository).save(second.getProduct());
        verify(stockEventPublisher).publishStockReservationCanceled(first);
        verify(stockEventPublisher).publishStockReservationCanceled(second);
    }

    @Test
    void cancelByOrderIdReleasesConfirmedReservationsAndRestoresStock() {
        StockReservation confirmed = TestFixtures.confirmedReservation();
        when(reservationRepository.findAllByOrderIdAndStatusIn(
                TestFixtures.ORDER_ID,
                List.of(ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED)
        )).thenReturn(List.of(confirmed));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelByOrderId(TestFixtures.ORDER_ID);

        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        assertThat(confirmed.getProduct().getStockQuantity()).isEqualTo(13);
        verify(productRepository).save(confirmed.getProduct());
        verify(stockEventPublisher).publishStockReservationCanceled(confirmed);
    }

    @Test
    void confirmAndCancelByOrderIdIgnoreWhenNoActiveReservationExists() {
        when(reservationRepository.findAllByOrderIdAndStatus(TestFixtures.ORDER_ID, ReservationStatus.ACTIVE))
                .thenReturn(List.of());
        when(reservationRepository.findAllByOrderIdAndStatusIn(
                TestFixtures.ORDER_ID,
                List.of(ReservationStatus.ACTIVE, ReservationStatus.CONFIRMED)
        )).thenReturn(List.of());

        service.confirmByOrderId(TestFixtures.ORDER_ID);
        service.cancelByOrderId(TestFixtures.ORDER_ID);

        verify(reservationRepository, never()).save(any(StockReservation.class));
        verifyNoInteractions(productRepository);
        verifyNoInteractions(stockEventPublisher);
    }

    @Test
    void deleteRequiresExistingReservation() {
        UUID id = TestFixtures.RESERVATION_ID;
        when(reservationRepository.findById(id)).thenReturn(Optional.of(TestFixtures.activeReservation()), Optional.empty());

        service.delete(id);

        verify(reservationRepository).deleteById(id);
        assertThatExceptionOfType(StockReservationNotFoundException.class)
                .isThrownBy(() -> service.delete(id))
                .withMessage("Stock reservation not found with id: " + id);
    }

    @Test
    void statusOperationsThrowWhenReservationIsMissing() {
        when(reservationRepository.findById(TestFixtures.RESERVATION_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(StockReservationNotFoundException.class)
                .isThrownBy(() -> service.confirm(TestFixtures.RESERVATION_ID))
                .withMessage("Stock reservation not found with id: " + TestFixtures.RESERVATION_ID);
        assertThatExceptionOfType(StockReservationNotFoundException.class)
                .isThrownBy(() -> service.cancel(TestFixtures.RESERVATION_ID))
                .withMessage("Stock reservation not found with id: " + TestFixtures.RESERVATION_ID);
        assertThatExceptionOfType(StockReservationNotFoundException.class)
                .isThrownBy(() -> service.expire(TestFixtures.RESERVATION_ID))
                .withMessage("Stock reservation not found with id: " + TestFixtures.RESERVATION_ID);
        verifyNoInteractions(stockEventPublisher);
    }
}
