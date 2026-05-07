package br.com.accenture.customer.domain.pagination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortTest {

    @Test
    void asc_shouldBuildAscendingSort() {
        Sort sort = Sort.asc("name");
        assertThat(sort.field()).isEqualTo("name");
        assertThat(sort.direction()).isEqualTo(Direction.ASC);
    }

    @Test
    void desc_shouldBuildDescendingSort() {
        Sort sort = Sort.desc("createdAt");
        assertThat(sort.field()).isEqualTo("createdAt");
        assertThat(sort.direction()).isEqualTo(Direction.DESC);
    }

    @Test
    void shouldDefaultNullDirectionToAsc() {
        Sort sort = new Sort("name", null);
        assertThat(sort.direction()).isEqualTo(Direction.ASC);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void shouldRejectBlankField(String value) {
        assertThatThrownBy(() -> new Sort(value, Direction.ASC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field");
    }

}
