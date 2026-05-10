package br.com.accenture.payment.domain.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PageRequestTest {

    @Test
    void createsPageRequestWithDefaultsAndCopiesSorts() {
        PageRequest noSort = PageRequest.of(0, 20);
        PageRequest nullSort = new PageRequest(1, 10, null);
        PageRequest sorted = PageRequest.of(0, 5, List.of(Sort.desc("createdAt")));

        assertThat(noSort.sorts()).isEmpty();
        assertThat(nullSort.sorts()).isEmpty();
        assertThat(sorted.sorts()).containsExactly(Sort.desc("createdAt"));
    }

    @Test
    void rejectsInvalidPageAndSize() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PageRequest.of(-1, 10))
                .withMessage("page must be >= 0");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PageRequest.of(0, 0))
                .withMessage("size must be > 0");
    }
}
