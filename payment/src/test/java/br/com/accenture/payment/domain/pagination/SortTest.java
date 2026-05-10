package br.com.accenture.payment.domain.pagination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SortTest {

    @Test
    void createsAscendingDescendingAndDefaultsNullDirectionToAscending() {
        assertThat(Sort.asc("amount").direction()).isEqualTo(Direction.ASC);
        assertThat(Sort.desc("createdAt").direction()).isEqualTo(Direction.DESC);
        assertThat(new Sort("status", null).direction()).isEqualTo(Direction.ASC);
    }

    @Test
    void rejectsBlankField() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Sort.asc(" "))
                .withMessage("field must not be blank");
    }
}
