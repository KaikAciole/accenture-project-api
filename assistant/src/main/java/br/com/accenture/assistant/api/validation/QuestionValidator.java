package br.com.accenture.assistant.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class QuestionValidator implements ConstraintValidator<ValidQuestion, String> {

    private static final Pattern DISALLOWED = Pattern.compile(
            "[\\p{Cc}&&[^\\n\\r\\t]]|[\\u200B-\\u200D\\uFEFF]|[\\u202A-\\u202E]|[\\u2066-\\u2069]"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !DISALLOWED.matcher(value).find();
    }
}
