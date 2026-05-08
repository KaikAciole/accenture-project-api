package br.com.accenture.inventory.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PaginationTest {

    @Test
    void pageRequestNormalizesNullSortsAndValidatesBounds() {
        PageRequest pageRequest = new PageRequest(0, 20, null);

        assertThat(pageRequest.sorts()).isEmpty();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> PageRequest.of(-1, 20))
                .withMessage("page must be >= 0");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> PageRequest.of(0, 0))
                .withMessage("size must be > 0");
    }

    @Test
    void sortDefaultsToAscendingAndValidatesField() {
        assertThat(new Sort("name", null).direction()).isEqualTo(Direction.ASC);
        assertThat(Sort.asc("name")).isEqualTo(new Sort("name", Direction.ASC));
        assertThat(Sort.desc("name")).isEqualTo(new Sort("name", Direction.DESC));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Sort(null, Direction.ASC))
                .withMessage("field must not be blank");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Sort(" ", Direction.ASC))
                .withMessage("field must not be blank");
    }

    @Test
    void pageResultSupportsNavigationAndMapping() {
        PageResult<String> page = new PageResult<>(List.of("a", "bb"), 1, 2, 5, 3);

        PageResult<Integer> mapped = page.map(String::length);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isTrue();
        assertThat(mapped.content()).containsExactly(1, 2);
        assertThat(mapped.pageNumber()).isEqualTo(1);
        assertThat(new PageResult<>(null, 0, 10, 0, 0).content()).isEmpty();
        assertThat(new PageResult<>(List.of("last"), 2, 2, 5, 3).hasNext()).isFalse();
        assertThat(new PageResult<>(List.of("first"), 0, 2, 5, 3).hasPrevious()).isFalse();
    }
}
