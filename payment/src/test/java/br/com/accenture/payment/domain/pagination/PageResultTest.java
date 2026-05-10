package br.com.accenture.payment.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void normalizesContentAndReportsNavigation() {
        PageResult<String> first = new PageResult<>(null, 0, 2, 3, 2);
        PageResult<String> second = new PageResult<>(List.of("a"), 1, 2, 3, 2);

        assertThat(first.content()).isEmpty();
        assertThat(first.hasNext()).isTrue();
        assertThat(first.hasPrevious()).isFalse();
        assertThat(second.hasNext()).isFalse();
        assertThat(second.hasPrevious()).isTrue();
    }

    @Test
    void mapsContentPreservingPageMetadata() {
        PageResult<String> page = new PageResult<>(List.of("abc"), 2, 10, 21, 3);

        PageResult<Integer> mapped = page.map(String::length);

        assertThat(mapped.content()).containsExactly(3);
        assertThat(mapped.pageNumber()).isEqualTo(2);
        assertThat(mapped.pageSize()).isEqualTo(10);
        assertThat(mapped.totalElements()).isEqualTo(21);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }
}
