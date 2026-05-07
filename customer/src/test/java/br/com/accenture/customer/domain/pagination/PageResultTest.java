package br.com.accenture.customer.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void hasNext_shouldBeTrueWhenNotLastPage() {
        PageResult<String> result = new PageResult<>(List.of("a"), 0, 1, 3, 3);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void hasNext_shouldBeFalseOnLastPage() {
        PageResult<String> result = new PageResult<>(List.of("c"), 2, 1, 3, 3);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void hasPrevious_shouldBeTrueAfterFirstPage() {
        PageResult<String> result = new PageResult<>(List.of("b"), 1, 1, 3, 3);
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void hasPrevious_shouldBeFalseOnFirstPage() {
        PageResult<String> result = new PageResult<>(List.of("a"), 0, 1, 3, 3);
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void map_shouldTransformContentPreservingMetadata() {
        PageResult<Integer> source = new PageResult<>(List.of(1, 2, 3), 1, 3, 9, 3);

        PageResult<String> mapped = source.map(i -> "v" + i);

        assertThat(mapped.content()).containsExactly("v1", "v2", "v3");
        assertThat(mapped.pageNumber()).isEqualTo(1);
        assertThat(mapped.pageSize()).isEqualTo(3);
        assertThat(mapped.totalElements()).isEqualTo(9);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }

    @Test
    void compactConstructor_shouldDefaultNullContentToEmpty() {
        PageResult<String> result = new PageResult<>(null, 0, 10, 0, 0);
        assertThat(result.content()).isEmpty();
    }

    @Test
    void contentList_shouldBeImmutable() {
        var mutable = new java.util.ArrayList<>(List.of("a"));
        PageResult<String> result = new PageResult<>(mutable, 0, 10, 1, 1);
        mutable.clear();
        assertThat(result.content()).hasSize(1);
    }

}
