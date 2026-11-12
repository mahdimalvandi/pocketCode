package dev.pocketcode.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class SensitiveActionClassifierTest {
    @Test public void catchesEnglishSideEffectsAndResourceIds() {
        assertTrue(SensitiveActionClassifier.requiresApproval("Send message"));
        assertTrue(SensitiveActionClassifier.requiresApproval("com.telegram:id/message_send_button"));
        assertTrue(SensitiveActionClassifier.requiresApproval("Delete account"));
        assertTrue(SensitiveActionClassifier.requiresApproval("PaymentCheckout"));
    }

    @Test public void catchesPersianSideEffects() {
        assertTrue(SensitiveActionClassifier.requiresApproval("ارسال پیام"));
        assertTrue(SensitiveActionClassifier.requiresApproval("دکمه_حذف_فایل"));
        assertTrue(SensitiveActionClassifier.requiresApproval("برداشت از حساب"));
    }

    @Test public void ignoresOrdinaryNavigationLabels() {
        assertFalse(SensitiveActionClassifier.requiresApproval("Contacts"));
        assertFalse(SensitiveActionClassifier.requiresApproval("Play video"));
        assertTrue(SensitiveActionClassifier.requiresApproval("Payment history"));
    }
}
