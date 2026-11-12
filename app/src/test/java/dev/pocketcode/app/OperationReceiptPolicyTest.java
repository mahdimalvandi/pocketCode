package dev.pocketcode.app;

import org.junit.Test;
import static org.junit.Assert.*;

public final class OperationReceiptPolicyTest {
    @Test public void receiptsCoverSideEffectingActionsButNotReads() {
        for (String action : new String[]{"writeFile","deleteFile","writeShared","openApp",
                "click_element","type_text","scroll_element","phone_back","tap_screen"}) {
            assertTrue(action, OperationReceiptPolicy.requiresNativeReceipt(action));
        }
        for (String action : new String[]{"readFile","readShared","readPhoneStorage","listFiles",
                "read_screen","capture_screen","gemini","listApps"}) {
            assertFalse(action, OperationReceiptPolicy.requiresNativeReceipt(action));
        }
    }

    @Test public void operationIdsAreBoundedAndSafe() {
        assertTrue(OperationReceiptPolicy.isValidOperationId("pc:session-1:run_2:call.3"));
        assertTrue(OperationReceiptPolicy.isValidOperationId("manual-550e8400-e29b-41d4-a716-446655440000"));
        assertFalse(OperationReceiptPolicy.isValidOperationId(""));
        assertFalse(OperationReceiptPolicy.isValidOperationId("not safe / id"));
        assertFalse(OperationReceiptPolicy.isValidOperationId(new String(new char[129]).replace('\0','x')));
    }

    @Test public void sameIdCannotBeReusedForAnotherNativeAction() {
        assertTrue(OperationReceiptPolicy.sameAction("writeFile","writeFile"));
        assertFalse(OperationReceiptPolicy.sameAction("writeFile","deleteFile"));
        assertFalse(OperationReceiptPolicy.sameAction(null,"writeFile"));
    }

    @Test public void onlyKnownPreflightFailuresAreMarkedFailed() {
        assertEquals("completed",OperationReceiptPolicy.failureState(null));
        assertEquals("failed",OperationReceiptPolicy.failureState("User denied this operation."));
        assertEquals("failed",OperationReceiptPolicy.failureState("Invalid app package."));
        assertEquals("unknown",OperationReceiptPolicy.failureState("Native action returned false."));
        assertEquals("unknown",OperationReceiptPolicy.failureState("The operation could not be completed."));
        assertEquals("unknown",OperationReceiptPolicy.failureState("I/O interrupted during write."));
    }

    @Test public void onlyPriorProcessStartedOperationsBecomeUnknownOnRecovery() {
        assertTrue(OperationReceiptPolicy.shouldRecoverInterrupted("started","process-a","process-b"));
        assertFalse(OperationReceiptPolicy.shouldRecoverInterrupted("started","process-a","process-a"));
        assertFalse(OperationReceiptPolicy.shouldRecoverInterrupted("completed","process-a","process-b"));
    }
}
