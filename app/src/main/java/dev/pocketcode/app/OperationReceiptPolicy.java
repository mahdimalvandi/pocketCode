package dev.pocketcode.app;

import java.util.Locale;

/** Pure policy helpers for durable native operation receipts. */
public final class OperationReceiptPolicy {
    public enum Decision { NEW, DUPLICATE, CONFLICT }

    private OperationReceiptPolicy() {}

    public static boolean requiresNativeReceipt(String action) {
        if (action == null) return false;
        switch (action) {
            case "writeFile":
            case "deleteFile":
            case "writeShared":
            case "openApp":
            case "click_element":
            case "type_text":
            case "scroll_element":
            case "phone_back":
            case "tap_screen":
                return true;
            default:
                return false;
        }
    }

    public static boolean isValidOperationId(String operationId) {
        if (operationId == null || operationId.isEmpty() || operationId.length() > 128) return false;
        for (int i = 0; i < operationId.length(); i++) {
            char c = operationId.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')
                    && !(c >= '0' && c <= '9') && c != '.' && c != '_' && c != ':' && c != '-') return false;
        }
        return true;
    }

    public static boolean sameAction(String existingAction, String requestedAction) {
        return existingAction != null && existingAction.equals(requestedAction);
    }

    /** Only errors known to happen before an effect may be recorded as failed. */
    public static String failureState(String error) {
        if (error == null) return "completed";
        String value = error.toLowerCase(Locale.ROOT);
        return definitelyNotExecuted(value) ? "failed" : "unknown";
    }

    public static boolean shouldRecoverInterrupted(String state, String storedProcess, String currentProcess) {
        return "started".equals(state) && storedProcess != null && !storedProcess.equals(currentProcess);
    }

    private static boolean definitelyNotExecuted(String value) {
        return value.contains("user denied")
                || value.contains("denied this")
                || value.contains("user cancelled")
                || value.contains("user canceled")
                || value.contains("user stopped")
                || value.contains("automation stopped")
                || value.contains("permission policy")
                || value.contains("policy blocks")
                || value.contains("blocked")
                || value.contains("invalid")
                || value.contains("not installed")
                || value.contains("no readable screen")
                || value.contains("element expired")
                || value.contains("element changed")
                || value.contains("not editable")
                || value.contains("not scrollable")
                || value.contains("no clickable ancestor")
                || value.contains("screenshot is missing")
                || value.contains("no action performed")
                || value.contains("another approval is pending")
                || value.contains("return to pocket code to approve")
                || value.contains("too many requests")
                || value.contains("app is busy")
                || value.contains("file could not be deleted")
                || value.contains("no action was executed")
                || value.contains("operation is too large")
                || value.contains("not supported")
                || value.contains("permission")
                || value.contains("مجوز")
                || value.contains("دسترسی");
    }
}
