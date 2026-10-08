package com.energymonitor.device.domain.model;

/**
 * Argument checks shared by the Device domain model.
 */
final class Preconditions {

    private Preconditions() {
    }

    static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    static String text(String value, String field) {
        notNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    static String text(String value, int maxLength, String field) {
        text(value, field);
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return value;
    }

    static String optionalText(String value, int maxLength, String field) {
        if (value == null) {
            return null;
        }
        return text(value, maxLength, field);
    }
}
