package com.example.demo;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * Input Validation System using Chain of Responsibility Design Pattern
 *
 * This implementation allows flexible validation rules that can be easily extended,
 * modified, or reordered without changing the core validation logic.
 */

// ============ VALIDATION RESULT CLASS ============

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

// ============ CHAIN OF RESPONSIBILITY PATTERN ============

/**
 * Abstract base class for validation handlers
 */
abstract class ValidationHandler {
    protected ValidationHandler nextHandler;

    /**
     * Set the next handler in the chain
     */
    public ValidationHandler setNext(ValidationHandler handler) {
        this.nextHandler = handler;
        return handler;
    }

    /**
     * Handle the validation request
     */
    public ValidationResult handle(String input) {
        ValidationResult result = validate(input);

        // If validation fails, stop the chain
        if (!result.isValid()) {
            return result;
        }

        // If this is the last handler, return success
        if (nextHandler == null) {
            return result;
        }

        // Pass to next handler with potentially modified input
        String processedInput = result.getCleanedInput() != null ? result.getCleanedInput() : input;
        return nextHandler.handle(processedInput);
    }

    /**
     * Abstract method for specific validation logic
     */
    protected abstract ValidationResult validate(String input);
}

// ============ CONCRETE VALIDATION HANDLERS ============

/**
 * Handler to validate null inputs
 */
class NullValidationHandler extends ValidationHandler {
    @Override
    protected ValidationResult validate(String input) {
        if (input == null) {
            return ValidationResult.invalid("Input is null");
        }
        return ValidationResult.valid(input);
    }
}

/**
 * Handler to clean and validate basic input format
 */
class InputCleaningHandler extends ValidationHandler {
    @Override
    protected ValidationResult validate(String input) {
        // Clean the input
        String cleaned = input.trim()
                .replaceAll("\\s+", " ")  // Multiple spaces to single space
                .replaceAll("[\\r\\n]+", " "); // Line breaks to spaces

        // Check if cleaned input is empty
        if (cleaned.isEmpty()) {
            return ValidationResult.invalid("Input is empty after cleaning");
        }

        return ValidationResult.valid(cleaned);
    }
}

/**
 * Handler to validate input length
 */
class LengthValidationHandler extends ValidationHandler {
    private final int minLength;
    private final int maxLength;

    public LengthValidationHandler(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected ValidationResult validate(String input) {
        if (input.length() < minLength) {
            return ValidationResult.invalid("Input is too short (minimum " + minLength + " characters)");
        }

        if (input.length() > maxLength) {
            return ValidationResult.invalid("Input is too long (maximum " + maxLength + " characters)");
        }

        return ValidationResult.valid(input);
    }
}

/**
 * Handler to validate content patterns (spaces, punctuation only)
 */
class ContentPatternValidationHandler extends ValidationHandler {
    private static final Pattern ONLY_SPACES = Pattern.compile("^\\s*$");
    private static final Pattern ONLY_PUNCTUATION = Pattern.compile("^[\\p{Punct}\\s]*$");

    @Override
    protected ValidationResult validate(String input) {
        if (ONLY_SPACES.matcher(input).matches()) {
            return ValidationResult.invalid("Contains only spaces");
        }

        if (ONLY_PUNCTUATION.matcher(input).matches()) {
            return ValidationResult.invalid("Contains only punctuation");
        }

        return ValidationResult.valid(input);
    }
}

/**
 * Handler to validate spam patterns
 */
class SpamValidationHandler extends ValidationHandler {
    private final Pattern excessiveRepetition;
    private final Pattern spamPattern;

    public SpamValidationHandler(int maxRepeatedChars) {
        this.excessiveRepetition = Pattern.compile("(.)\\1{" + maxRepeatedChars + ",}");
        this.spamPattern = Pattern.compile("(.)\\1{3,}|([a-zA-Z]{1,3})\\2{5,}");
    }

    @Override
    protected ValidationResult validate(String input) {
        if (excessiveRepetition.matcher(input).find()) {
            return ValidationResult.invalid("Contains excessive character repetition");
        }

        if (spamPattern.matcher(input.toLowerCase()).find()) {
            return ValidationResult.invalid("Contains spam patterns");
        }

        return ValidationResult.valid(input);
    }
}

/**
 * Handler to validate against useless input lists
 */
class UselessInputValidationHandler extends ValidationHandler {
    private final List<String> uselessInputs;
    private final List<String> spamWords;

    public UselessInputValidationHandler(List<String> uselessInputs, List<String> spamWords) {
        this.uselessInputs = uselessInputs;
        this.spamWords = spamWords;
    }

    @Override
    protected ValidationResult validate(String input) {
        String lower = input.toLowerCase().trim();

        if (uselessInputs.contains(lower)) {
            return ValidationResult.invalid("Common input with no informational value");
        }

        if (spamWords.stream().anyMatch(lower::contains)) {
            return ValidationResult.invalid("Contains spam words");
        }

        return ValidationResult.valid(input);
    }
}

/**
 * Handler to validate content quality
 */
class ContentQualityValidationHandler extends ValidationHandler {
    private final double minLetterRatio;

    public ContentQualityValidationHandler(double minLetterRatio) {
        this.minLetterRatio = minLetterRatio;
    }

    @Override
    protected ValidationResult validate(String input) {
        // Must have at least one letter
        if (!input.matches(".*[a-zA-ZáéíóúñÁÉÍÓÚÑ].*")) {
            return ValidationResult.invalid("Does not contain valid letters");
        }

        // Calculate letter ratio
        long letterCount = input.chars().filter(Character::isLetter).count();
        double letterRatio = (double) letterCount / input.length();

        if (letterRatio < minLetterRatio) {
            return ValidationResult.invalid("Does not contain sufficient meaningful content");
        }

        return ValidationResult.valid(input);
    }
}

/**
 * Handler to estimate and validate token usage
 */
class TokenEstimationHandler extends ValidationHandler {
    private final int maxTokens;

    public TokenEstimationHandler(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    @Override
    protected ValidationResult validate(String input) {
        int estimatedTokens = Math.max(1, input.length() / 4); // Rough estimation

        if (estimatedTokens > maxTokens) {
            return ValidationResult.invalid("Input would consume too many tokens: " + estimatedTokens);
        }

        return ValidationResult.valid(input);
    }
}

// ============ MAIN VALIDATOR CLASS ============

/**
 * Main validator class using Chain of Responsibility pattern
 */
public class InputValidator {
    private ValidationHandler validationChain;

    /**
     * Constructor with custom validation chain
     */
    public InputValidator(ValidationHandler validationChain) {
        this.validationChain = validationChain;
    }

    /**
     * Default constructor with standard validation chain
     */
    public InputValidator() {
        this.validationChain = createDefaultValidationChain();
    }

    /**
     * Validates an input using the validation chain
     */
    public ValidationResult validateInput(String input) {
        return validationChain.handle(input);
    }

    /**
     * Quick validation check
     */
    public boolean isValidInput(String input) {
        return validateInput(input).isValid();
    }

    /**
     * Get cleaned input if valid
     */
    public String getCleanedInput(String input) {
        ValidationResult result = validateInput(input);
        return result.isValid() ? result.getCleanedInput() : null;
    }

    /**
     * Creates the default validation chain
     */
    private ValidationHandler createDefaultValidationChain() {
        // Default configuration
        List<String> uselessInputs = Arrays.asList(
                "hello", "hi", "hey", "ok", "okay", "yes", "no", "nothing", "hola", "si",
                "test", "testing", "?", "??", "???", "...", "hahaha", "haha", "lol",
                "lmao", "xd", "asd", "asdf", "qwerty", "123", "abc", "nada", "prueba"
        );

        List<String> spamWords = Arrays.asList(
                "aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee", "fffff", "ggggg",
                "hhhhh", "iiiii", "jjjjj", "kkkkk", "lllll", "mmmmm", "nnnnn"
        );

        // Create handlers
        ValidationHandler nullHandler = new NullValidationHandler();
        ValidationHandler cleaningHandler = new InputCleaningHandler();
        ValidationHandler lengthHandler = new LengthValidationHandler(2, 4000);
        ValidationHandler contentHandler = new ContentPatternValidationHandler();
        ValidationHandler spamHandler = new SpamValidationHandler(10);
        ValidationHandler uselessHandler = new UselessInputValidationHandler(uselessInputs, spamWords);
        ValidationHandler qualityHandler = new ContentQualityValidationHandler(0.3);
        ValidationHandler tokenHandler = new TokenEstimationHandler(1000);

        // Chain the handlers in order
        nullHandler
                .setNext(cleaningHandler)
                .setNext(lengthHandler)
                .setNext(contentHandler)
                .setNext(spamHandler)
                .setNext(uselessHandler)
                .setNext(qualityHandler)
                .setNext(tokenHandler);

        return nullHandler;
    }

    // ============ FACTORY METHODS FOR DIFFERENT CONFIGURATIONS ============

    /**
     * Creates a strict validator with tighter rules
     */
    public static InputValidator createStrict() {
        List<String> uselessInputs = Arrays.asList(
                "hello", "hi", "hey", "ok", "okay", "yes", "no", "nothing", "hola", "si",
                "test", "testing", "?", "??", "???", "...", "hahaha", "haha", "lol",
                "lmao", "xd", "asd", "asdf", "qwerty", "123", "abc", "nada", "prueba",
                "how are you", "what's up", "como estas", "que tal"
        );

        List<String> spamWords = Arrays.asList(
                "aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee", "fffff", "ggggg",
                "hhhhh", "iiiii", "jjjjj", "kkkkk", "lllll", "mmmmm", "nnnnn"
        );

        ValidationHandler chain = new NullValidationHandler();
        chain.setNext(new InputCleaningHandler())
                .setNext(new LengthValidationHandler(5, 2000))
                .setNext(new ContentPatternValidationHandler())
                .setNext(new SpamValidationHandler(5))
                .setNext(new UselessInputValidationHandler(uselessInputs, spamWords))
                .setNext(new ContentQualityValidationHandler(0.5))
                .setNext(new TokenEstimationHandler(500));

        return new InputValidator(chain);
    }

    /**
     * Creates a lenient validator with relaxed rules
     */
    public static InputValidator createLenient() {
        List<String> uselessInputs = Arrays.asList(
                "test", "testing", "asd", "asdf", "qwerty", "123"
        );

        List<String> spamWords = Arrays.asList(
                "aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee"
        );

        ValidationHandler chain = new NullValidationHandler();
        chain.setNext(new InputCleaningHandler())
                .setNext(new LengthValidationHandler(1, 8000))
                .setNext(new ContentPatternValidationHandler())
                .setNext(new SpamValidationHandler(20))
                .setNext(new UselessInputValidationHandler(uselessInputs, spamWords))
                .setNext(new ContentQualityValidationHandler(0.1))
                .setNext(new TokenEstimationHandler(2000));

        return new InputValidator(chain);
    }

    /**
     * Creates a custom validation chain builder
     */
    public static ValidationChainBuilder builder() {
        return new ValidationChainBuilder();
    }

    // ============ BUILDER FOR CUSTOM CHAINS ============

    /**
     * Builder class for creating custom validation chains
     */
    public static class ValidationChainBuilder {
        private ValidationHandler firstHandler;
        private ValidationHandler lastHandler;

        public ValidationChainBuilder addNullValidation() {
            return addHandler(new NullValidationHandler());
        }

        public ValidationChainBuilder addInputCleaning() {
            return addHandler(new InputCleaningHandler());
        }

        public ValidationChainBuilder addLengthValidation(int minLength, int maxLength) {
            return addHandler(new LengthValidationHandler(minLength, maxLength));
        }

        public ValidationChainBuilder addContentPatternValidation() {
            return addHandler(new ContentPatternValidationHandler());
        }

        public ValidationChainBuilder addSpamValidation(int maxRepeatedChars) {
            return addHandler(new SpamValidationHandler(maxRepeatedChars));
        }

        public ValidationChainBuilder addUselessInputValidation(List<String> uselessInputs, List<String> spamWords) {
            return addHandler(new UselessInputValidationHandler(uselessInputs, spamWords));
        }

        public ValidationChainBuilder addContentQualityValidation(double minLetterRatio) {
            return addHandler(new ContentQualityValidationHandler(minLetterRatio));
        }

        public ValidationChainBuilder addTokenEstimation(int maxTokens) {
            return addHandler(new TokenEstimationHandler(maxTokens));
        }

        public ValidationChainBuilder addCustomHandler(ValidationHandler handler) {
            return addHandler(handler);
        }

        private ValidationChainBuilder addHandler(ValidationHandler handler) {
            if (firstHandler == null) {
                firstHandler = handler;
                lastHandler = handler;
            } else {
                lastHandler.setNext(handler);
                lastHandler = handler;
            }
            return this;
        }

        public InputValidator build() {
            if (firstHandler == null) {
                throw new IllegalStateException("At least one validation handler must be added");
            }
            return new InputValidator(firstHandler);
        }
    }
}

