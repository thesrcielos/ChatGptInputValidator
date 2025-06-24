package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputValidatorTest {
    @Test
    void testValidInput() {
        InputValidator validator = new InputValidator();
        ValidationResult result = validator.validateInput("This is a valid sentence.");
        assertTrue(result.isValid());
    }

}
