package util;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared validation and normalization rules for posts and comments.
 * Controllers and repositories call this so behavior stays consistent.
 */
/*
 * ===============================
 * POST VALIDATION RULES
 * ===============================
 *
 * - Title is required
 * - Title must be between 3 and 80 characters
 * - Title cannot contain only symbols or spaces
 * - Title must contain at least one letter or digit
 *
 * - Content is required
 * - Content must be between 10 and 5000 characters
 *
 * - Category is optional
 * - If provided, category must be between 2 and 30 characters
 * - Category must start with '#'
 * - Category may contain letters, numbers, spaces, '_' or '-'
 *
 */
/*
 * ===============================
 * COMMENT VALIDATION RULES
 * ===============================
 *
 * - Comment is required
 * - Comment must be between 2 and 1000 characters
 *
 */

public final class InputValidator {

    private InputValidator() {
    }

    // Post rules
    public static final int POST_TITLE_MIN = 3;
    public static final int POST_TITLE_MAX = 80;
    public static final int POST_CONTENT_MIN = 10;
    public static final int POST_CONTENT_MAX = 5000;

    public static final int CAT_MIN = 2;
    public static final int CAT_MAX = 30;

    // # + 1..29 allowed chars = total 2..30
    private static final String CATEGORY_REGEX = "^#[A-Za-z0-9 _-]{1,29}$";

    // Comment rules
    public static final int COMMENT_MIN = 2;
    public static final int COMMENT_MAX = 1000;

    // Validation path for post create/update actions across user/admin CRUD screens.
    public static List<String> validatePost(String title, String content, String category) {
        List<String> errors = new ArrayList<>();

        String t = norm(title);
        String c = norm(content);
        String cat = norm(category);

        // title
        if (t == null) {
            errors.add("Title is required.");
        } else {
            if (t.length() < POST_TITLE_MIN || t.length() > POST_TITLE_MAX) {
                errors.add("Title must be between " + POST_TITLE_MIN + " and " + POST_TITLE_MAX + " characters.");
            }
            if (isOnlySymbolsOrSpaces(t)) {
                errors.add("Title cannot be only symbols/spaces.");
            }
        }

        // content
        if (c == null) {
            errors.add("Content is required.");
        } else if (c.length() < POST_CONTENT_MIN || c.length() > POST_CONTENT_MAX) {
            errors.add("Content must be between " + POST_CONTENT_MIN + " and " + POST_CONTENT_MAX + " characters.");
        }

        // category optional
        if (cat != null) {
            if (cat.length() < CAT_MIN || cat.length() > CAT_MAX) {
                errors.add("Category must be between " + CAT_MIN + " and " + CAT_MAX + " characters.");
            } else if (!cat.matches(CATEGORY_REGEX)) {
                errors.add("Category must start with # and contain only letters, numbers, spaces, _ or -");
            }
        }

        return errors;
    }

    // Validation path for comment create/update actions.
    public static List<String> validateComment(String content) {
        List<String> errors = new ArrayList<>();
        String c = norm(content);

        if (c == null) {
            errors.add("Comment is required.");
        } else if (c.length() < COMMENT_MIN || c.length() > COMMENT_MAX) {
            errors.add("Comment must be between " + COMMENT_MIN + " and " + COMMENT_MAX + " characters.");
        }

        return errors;
    }

    /** trimmed, returns null if empty */
    public static String norm(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static boolean isOnlySymbolsOrSpaces(String s) {
        // must contain at least one letter or digit
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetterOrDigit(s.charAt(i)))
                return false;
        }
        return true;
    }

    /** returns null if empty; otherwise trimmed */
    public static String normalizeNullable(String s) {
        return norm(s);
    }

    // Useful for UI bindings when null is less convenient than empty text.
    /** returns empty string if null; otherwise trimmed */
    public static String normalize(String s) {
        if (s == null)
            return "";
        return s.trim();
    }
}
