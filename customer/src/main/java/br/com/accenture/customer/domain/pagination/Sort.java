package br.com.accenture.customer.domain.pagination;

public record Sort(String field, Direction direction) {

    public Sort {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
        if (direction == null) {
            direction = Direction.ASC;
        }
    }

    public static Sort asc(String field) {
        return new Sort(field, Direction.ASC);
    }

    public static Sort desc(String field) {
        return new Sort(field, Direction.DESC);
    }

}
