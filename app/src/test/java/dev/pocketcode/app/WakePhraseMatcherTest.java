package dev.pocketcode.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class WakePhraseMatcherTest {
    @Test public void matchesEnglishPhraseIgnoringCaseAndPunctuation() {
        assertTrue(WakePhraseMatcher.matches("Please wake up: POCKET-CODE!"));
    }

    @Test public void matchesPersianPhraseAndSpacingVariants() {
        assertTrue(WakePhraseMatcher.matches("لطفاً پاکت کد"));
        assertTrue(WakePhraseMatcher.matches("پاکت‌کد"));
        assertTrue(WakePhraseMatcher.matches("پاکت، کد"));
    }

    @Test public void normalizesArabicYehAndKaf() {
        assertTrue(WakePhraseMatcher.matches("پاكت كد"));
        assertTrue(WakePhraseMatcher.matches("پاکت كد"));
    }

    @Test public void allowsCommonSpeechRecognitionVariant() {
        assertTrue(WakePhraseMatcher.matches("پاکت کود"));
    }

    @Test public void rejectsUnrelatedOrPartialPhrases() {
        assertFalse(WakePhraseMatcher.matches("Pocket"));
        assertFalse(WakePhraseMatcher.matches("Code"));
        assertFalse(WakePhraseMatcher.matches("کد پاکت"));
        assertFalse(WakePhraseMatcher.matches("سلام، حالت چطوره؟"));
        assertFalse(WakePhraseMatcher.matches(null));
    }
}
