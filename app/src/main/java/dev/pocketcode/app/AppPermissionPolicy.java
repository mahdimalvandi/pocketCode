package dev.pocketcode.app;

/** Package-scoped permissions for cross-app Accessibility operations. */
public final class AppPermissionPolicy {
    public static final String READ_ONLY = "read_only";
    public static final String NAVIGATION = "navigation";
    public static final String CONFIRM_ACTIONS = "confirm_actions";
    public static final String BLOCKED = "blocked";
    public static final String DEFAULT_POLICY = CONFIRM_ACTIONS;

    private AppPermissionPolicy() {}

    /** Mirrors the immutable package deny-list used by the Accessibility host. */
    public static boolean isBuiltInBlockedPackage(String packageName, String ownPackage) {
        if (packageName == null || packageName.trim().isEmpty()) return true;
        if (packageName.equals(ownPackage)) return true;
        String value = packageName.toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("com.android.") || value.contains("systemui")
                || value.contains("permissioncontroller") || value.contains("settings")
                || value.contains("keyboard") || value.contains("inputmethod")
                || value.contains("authenticator") || value.contains("bank")
                || value.contains("wallet") || value.contains("payment")
                || value.contains("password");
    }

    public static boolean isValidPolicy(String policy) {
        return READ_ONLY.equals(policy) || NAVIGATION.equals(policy)
                || CONFIRM_ACTIONS.equals(policy) || BLOCKED.equals(policy);
    }

    /** Unknown/corrupt values fail closed to read-only. */
    public static String normalize(String policy) {
        return isValidPolicy(policy) ? policy : READ_ONLY;
    }

    public static boolean allows(String policy, String action) {
        String mode = normalize(policy);
        if (BLOCKED.equals(mode)) return false;
        if ("open_app".equals(action)) return true;
        if ("read_screen".equals(action) || "capture_screen".equals(action)) return true;
        if (NAVIGATION.equals(mode) || CONFIRM_ACTIONS.equals(mode)) {
            if ("scroll_element".equals(action) || "phone_back".equals(action)) return true;
        }
        if (CONFIRM_ACTIONS.equals(mode)) {
            return "click_element".equals(action) || "type_text".equals(action)
                    || "tap_screen".equals(action);
        }
        return false;
    }

    /** State-changing interactions always show a native, per-operation approval in this mode. */
    public static boolean requiresApproval(String policy, String action) {
        return CONFIRM_ACTIONS.equals(normalize(policy))
                && ("click_element".equals(action) || "type_text".equals(action)
                    || "tap_screen".equals(action));
    }
}
