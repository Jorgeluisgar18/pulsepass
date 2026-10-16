package com.pulsepass.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator
        implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_CHARACTERS = 12;
    private static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(
            String value,
            ConstraintValidatorContext context
    ) {

        if (value == null || value.isBlank()) {
            return true;
        }

        int byteLength =
                value.getBytes(
                        StandardCharsets.UTF_8
                ).length;

        return value.length() >= MIN_CHARACTERS
                && byteLength <= MAX_BYTES;
    }
}