package com.example.demo;

/**
 * Result of validation with factory methods
 */
public class ValidationResult {
    private final boolean isValid;
    private final String reason;
    private final String cleanedInput;

    private ValidationResult(boolean isValid, String reason, String cleanedInput) {
        this.isValid = isValid;
        this.reason = reason;
        this.cleanedInput = cleanedInput;
    }

    public static ValidationResult valid(String cleanedInput) {
        return new ValidationResult(true, null, cleanedInput);
    }

    public static ValidationResult invalid(String reason) {
        return new ValidationResult(false, reason, null);
    }

    public boolean isValid() { return isValid; }
    public String getReason() { return reason; }
    public String getCleanedInput() { return cleanedInput; }
}
