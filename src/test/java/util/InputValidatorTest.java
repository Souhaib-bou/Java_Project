package util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for validation boundaries and normalization helpers.
 * These tests lock expected behavior used by post/comment CRUD flows.
 */
class InputValidatorTest {

    // ---------- Helpers ----------
    // Builds deterministic boundary-length strings for edge-case tests.
    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(Math.max(0, n));
    }

    // Assertion helper used by negative test cases.
    private static void assertHasError(List<String> errors, String contains) {
        assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains(contains.toLowerCase())),
                "Expected an error containing: " + contains + "\nActual: " + errors);
    }

    // Assertion helper used by happy-path test cases.
    private static void assertNoErrors(List<String> errors) {
        assertTrue(errors == null || errors.isEmpty(), "Expected no errors, but got: " + errors);
    }

    // ---------- POST: Title ----------
    @Test
    void validatePost_titleEmpty_fails() {
        List<String> errors = InputValidator.validatePost("", "valid content here!", "#job");
        assertHasError(errors, "title");
    }

    @Test
    void validatePost_titleTooShort_fails() {
        List<String> errors = InputValidator.validatePost("Hi", "valid content here!", "#job");
        assertHasError(errors, "title");
        assertHasError(errors, "3");
    }

    @Test
    void validatePost_titleTooLong_fails() {
        String title81 = repeat('a', 81);
        List<String> errors = InputValidator.validatePost(title81, "valid content here!", "#job");
        assertHasError(errors, "title");
        assertHasError(errors, "80");
    }

    @Test
    void validatePost_titleAtMin_ok() {
        List<String> errors = InputValidator.validatePost("Hey", "valid content here!", "#job");
        assertNoErrors(errors);
    }

    @Test
    void validatePost_titleAtMax_ok() {
        String title80 = repeat('a', 80);
        List<String> errors = InputValidator.validatePost(title80, "valid content here!", "#job");
        assertNoErrors(errors);
    }

    // ---------- POST: Content ----------
    @Test
    void validatePost_contentEmpty_fails() {
        List<String> errors = InputValidator.validatePost("Good title", "", "#job");
        assertHasError(errors, "content");
    }

    @Test
    void validatePost_contentTooShort_fails() {
        List<String> errors = InputValidator.validatePost("Good title", "123456789", "#job"); // 9 chars
        assertHasError(errors, "content");
        assertHasError(errors, "10");
    }

    @Test
    void validatePost_contentAtMin_ok() {
        List<String> errors = InputValidator.validatePost("Good title", "1234567890", "#job"); // 10 chars
        assertNoErrors(errors);
    }

    @Test
    void validatePost_contentTooLong_fails() {
        String big = repeat('x', 5001);
        List<String> errors = InputValidator.validatePost("Good title", big, "#job");
        assertHasError(errors, "content");
        assertHasError(errors, "5000");
    }

    @Test
    void validatePost_contentAtMax_ok() {
        String big = repeat('x', 5000);
        List<String> errors = InputValidator.validatePost("Good title", big, "#job");
        assertNoErrors(errors);
    }

    // ---------- POST: Category ----------
    @Test
    void validatePost_categoryNull_ok() {
        List<String> errors = InputValidator.validatePost("Good title", "valid content here!", null);
        assertNoErrors(errors);
    }

    @Test
    void validatePost_categoryEmpty_ok() {
        List<String> errors = InputValidator.validatePost("Good title", "valid content here!", "");
        // Depending on your validator, "" might be treated as invalid or normalized to
        // null.
        // If your validator expects normalization, keep this as OK.
        // If it treats empty as invalid, change to assertHasError(errors, "category");
        assertNoErrors(errors);
    }

    @Test
    void validatePost_categoryMustStartWithHash_fails() {
        List<String> errors = InputValidator.validatePost("Good title", "valid content here!", "investment");
        assertHasError(errors, "category");
        assertHasError(errors, "#");
    }

    @Test
    void validatePost_categoryTooShort_fails() {
        List<String> errors = InputValidator.validatePost("Good title", "valid content here!", "#"); // too short
        assertHasError(errors, "category");
    }

    @Test
    void validatePost_categoryTooLong_fails() {
        // category length rule: 2–30 chars including '#'
        String cat = "#" + repeat('a', 30); // 31 including '#'
        List<String> errors = InputValidator.validatePost("Good title", "valid content here!", cat);
        assertHasError(errors, "category");
        assertHasError(errors, "30");
    }

    @Test
    void validatePost_categoryValid_ok() {
        List<String> errors = InputValidator.validatePost("Good title", "valid content here!", "#investment");
        assertNoErrors(errors);
    }

    // ---------- COMMENT ----------
    @Test
    void validateComment_empty_fails() {
        List<String> errors = InputValidator.validateComment("");
        assertHasError(errors, "comment");
    }

    @Test
    void validateComment_tooShort_fails() {
        List<String> errors = InputValidator.validateComment("a"); // 1 char
        assertHasError(errors, "2");
    }

    @Test
    void validateComment_atMin_ok() {
        List<String> errors = InputValidator.validateComment("ok"); // 2 chars
        assertNoErrors(errors);
    }

    @Test
    void validateComment_tooLong_fails() {
        String big = repeat('c', 1001);
        List<String> errors = InputValidator.validateComment(big);
        assertHasError(errors, "1000");
    }

    @Test
    void validateComment_atMax_ok() {
        String big = repeat('c', 1000);
        List<String> errors = InputValidator.validateComment(big);
        assertNoErrors(errors);
    }

    // ---------- Normalization ----------
    @Test
    void normalize_trimAndNullifyCategory() {
        // If your InputValidator has normalization helpers, test them here.
        // The method name may differ; adjust if needed.
        String normalized = InputValidator.normalizeNullable("   ");
        assertNull(normalized, "Expected whitespace-only category to normalize to null");

        String normalized2 = InputValidator.normalizeNullable("  #investment  ");
        assertEquals("#investment", normalized2);
    }

    @Test
    void normalize_trimTitleAndContent() {
        // Again, adjust method names if your class exposes different helpers.
        assertEquals("Hello", InputValidator.normalize("  Hello  "));
        assertEquals("", InputValidator.normalize(null));
    }
}
