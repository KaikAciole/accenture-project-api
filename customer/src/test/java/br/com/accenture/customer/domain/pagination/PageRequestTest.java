package br.com.accenture.customer.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @Test
    void of_shouldCreateRequestWithEmptySorts() {
        PageRequest request = PageRequest.of(0, 10);
        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(10);
        assertThat(request.sorts()).isEmpty();
    }

    @Test
    void of_shouldAcceptCustomSorts() {
        PageRequest request = PageRequest.of(2, 20, List.of(Sort.asc("name"), Sort.desc("createdAt")));
        assertThat(request.sorts()).hasSize(2);
        assertThat(request.sorts().get(0).field()).isEqualTo("name");
        assertThat(request.sorts().get(1).direction()).isEqualTo(Direction.DESC);
    }

    @Test
    void compactConstructor_shouldDefaultNullSortsToEmpty() {
        PageRequest request = new PageRequest(0, 10, null);
        assertThat(request.sorts()).isEmpty();
    }

    @Test
    void compactConstructor_shouldCopyDefensivelyTheSortsList() {
        var mutable = new java.util.ArrayList<>(List.of(Sort.asc("name")));
        PageRequest request = new PageRequest(0, 10, mutable);
        mutable.clear();
        assertThat(request.sorts()).hasSize(1);
    }

    @Test
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> PageRequest.of(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    void shouldRejectZeroSize() {
        assertThatThrownBy(() -> PageRequest.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void shouldRejectNegativeSize() {
        assertThatThrownBy(() -> PageRequest.of(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

}
