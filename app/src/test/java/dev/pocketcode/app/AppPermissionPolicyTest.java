package dev.pocketcode.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppPermissionPolicyTest {
    @Test public void readOnlyAllowsOnlyScreenReadingAndAppLaunch() {
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "read_screen"));
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "capture_screen"));
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "open_app"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "scroll_element"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "phone_back"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "click_element"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "type_text"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.READ_ONLY, "tap_screen"));
    }

    @Test public void navigationAddsOnlyScrollAndBack() {
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.NAVIGATION, "read_screen"));
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.NAVIGATION, "scroll_element"));
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.NAVIGATION, "phone_back"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.NAVIGATION, "click_element"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.NAVIGATION, "type_text"));
        assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.NAVIGATION, "tap_screen"));
    }

    @Test public void confirmActionsAllowsMutationsButRequiresNativeApproval() {
        for (String action : new String[]{"click_element", "type_text", "tap_screen"}) {
            assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.CONFIRM_ACTIONS, action));
            assertTrue(AppPermissionPolicy.requiresApproval(AppPermissionPolicy.CONFIRM_ACTIONS, action));
        }
        assertTrue(AppPermissionPolicy.allows(AppPermissionPolicy.CONFIRM_ACTIONS, "scroll_element"));
        assertFalse(AppPermissionPolicy.requiresApproval(AppPermissionPolicy.CONFIRM_ACTIONS, "scroll_element"));
        assertFalse(AppPermissionPolicy.requiresApproval(AppPermissionPolicy.CONFIRM_ACTIONS, "read_screen"));
    }

    @Test public void blockedDeniesEveryPhoneOperationIncludingLaunch() {
        for (String action : new String[]{"open_app", "read_screen", "capture_screen", "scroll_element",
                "phone_back", "click_element", "type_text", "tap_screen"}) {
            assertFalse(AppPermissionPolicy.allows(AppPermissionPolicy.BLOCKED, action));
        }
    }

    @Test public void builtInSensitivePackagesCannotBeOverridden() {
        assertTrue(AppPermissionPolicy.isBuiltInBlockedPackage("com.android.settings", "dev.pocketcode.app"));
        assertTrue(AppPermissionPolicy.isBuiltInBlockedPackage("com.example.wallet", "dev.pocketcode.app"));
        assertTrue(AppPermissionPolicy.isBuiltInBlockedPackage("dev.pocketcode.app", "dev.pocketcode.app"));
        assertFalse(AppPermissionPolicy.isBuiltInBlockedPackage("org.telegram.messenger", "dev.pocketcode.app"));
    }

    @Test public void invalidPolicyFailsClosed() {
        assertEquals(AppPermissionPolicy.READ_ONLY, AppPermissionPolicy.normalize("unknown"));
        assertFalse(AppPermissionPolicy.allows("unknown", "click_element"));
        assertFalse(AppPermissionPolicy.isValidPolicy("unknown"));
    }
}
