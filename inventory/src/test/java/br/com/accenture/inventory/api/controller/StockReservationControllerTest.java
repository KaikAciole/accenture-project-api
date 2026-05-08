package br.com.accenture.inventory.api.controller;

import br.com.accenture.inventory.api.dto.request.StockReservationRequest;
import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockReservationControllerTest {

    private final StockReservationService service = mock(StockReservationService.class);
    private final StockReservationController controller = new StockReservationController(service);

    @Test
    void createMapsRequestToServiceCall() {
        StockReservation reservation = TestFixtures.activeReservation();
        when(service.create(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID, 3)).thenReturn(reservation);

        var response = controller.create(new StockReservationRequest(TestFixtures.ORDER_ID, TestFixtures.PRODUCT_ID, 3));

        assertThat(response.id()).isEqualTo(TestFixtures.RESERVATION_ID);
        assertThat(response.productId()).isEqualTo(TestFixtures.PRODUCT_ID);
        assertThat(response.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void queryEndpointsMapDomainPages() {
        StockReservation reservation = TestFixtures.activeReservation();
        PageResult<StockReservation> page = new PageResult<>(List.of(reservation), 0, 20, 1, 1);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(service.findAll(any(PageRequest.class))).thenReturn(page);
        when(service.findByOrderId(eq(TestFixtures.ORDER_ID), any(PageRequest.class))).thenReturn(page);
        when(service.findByOrderIdAndStatus(eq(TestFixtures.ORDER_ID), eq(ReservationStatus.ACTIVE), any(PageRequest.class))).thenReturn(page);
        when(service.findByProductId(eq(TestFixtures.PRODUCT_ID), any(PageRequest.class))).thenReturn(page);
        when(service.findById(TestFixtures.RESERVATION_ID)).thenReturn(reservation);

        assertThat(controller.findAll(pageable).content()).hasSize(1);
        assertThat(controller.findByOrderId(TestFixtures.ORDER_ID, pageable).content()).hasSize(1);
        assertThat(controller.findByOrderIdAndStatus(TestFixtures.ORDER_ID, ReservationStatus.ACTIVE, pageable).content()).hasSize(1);
        assertThat(controller.findByProductId(TestFixtures.PRODUCT_ID, pageable).content()).hasSize(1);
        assertThat(controller.findById(TestFixtures.RESERVATION_ID).id()).isEqualTo(TestFixtures.RESERVATION_ID);
    }

    @Test
    void statusTransitionsAndDeleteDelegateToService() {
        when(service.confirm(TestFixtures.RESERVATION_ID)).thenReturn(TestFixtures.activeReservation());
        when(service.cancel(TestFixtures.RESERVATION_ID)).thenReturn(TestFixtures.activeReservation());
        when(service.expire(TestFixtures.RESERVATION_ID)).thenReturn(TestFixtures.activeReservation());

        assertThat(controller.confirm(TestFixtures.RESERVATION_ID).id()).isEqualTo(TestFixtures.RESERVATION_ID);
        assertThat(controller.cancel(TestFixtures.RESERVATION_ID).id()).isEqualTo(TestFixtures.RESERVATION_ID);
        assertThat(controller.expire(TestFixtures.RESERVATION_ID).id()).isEqualTo(TestFixtures.RESERVATION_ID);
        controller.delete(TestFixtures.RESERVATION_ID);

        verify(service).delete(TestFixtures.RESERVATION_ID);
    }
}
