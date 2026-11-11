package dev.pocketcode.app;

import java.util.Locale;

/** Normalizes Android recognition results before matching the configured wake phrase. */
final class WakePhraseMatcher {
    private WakePhraseMatcher() { }

    static boolean matches(String phrase) {
        if (phrase == null) return false;
        String normalized = phrase.toLowerCase(Locale.ROOT)
                .replace('ي', 'ی')
                .replace('ك', 'ک')
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return normalized.contains("pocketcode")
                || normalized.contains("پاکتکد")
                || normalized.contains("پاکتکود");
    }
}
