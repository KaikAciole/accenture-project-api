package br.com.accenture.assistant.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = QuestionValidator.class)
public @interface ValidQuestion {

    String message() default "question contains disallowed characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
