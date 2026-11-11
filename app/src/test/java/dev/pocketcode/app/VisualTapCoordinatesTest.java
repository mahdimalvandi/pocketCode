package dev.pocketcode.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisualTapCoordinatesTest {
    @Test public void mapsImagePixelCenterIntoWindowCoordinates() {
        float[] point=VisualTapCoordinates.map(50,25,100,50,
                10,20,210,120,10,20,210,120);
        assertEquals(111f,point[0],0.001f);
        assertEquals(71f,point[1],0.001f);
    }

    @Test public void mapsScaledDisplayImageAndKeepsTapInsideAppWindow() {
        float[] point=VisualTapCoordinates.map(400,500,720,1600,
                0,0,1080,2400,0,120,1080,2280);
        assertEquals(600.75f,point[0],0.01f);
        assertEquals(750.75f,point[1],0.01f);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsCoordinatesOutsideCapturedImage() {
        VisualTapCoordinates.map(100,20,100,50,0,0,100,50,0,0,100,50);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsDisplayCoordinatesOutsideActiveAppWindow() {
        // The screenshot includes the status bar, but taps are confined to the app window.
        VisualTapCoordinates.map(50,10,100,200,0,0,100,200,0,24,100,190);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsEmptyCoordinateSpace() {
        VisualTapCoordinates.map(1,1,0,200,0,0,100,200,0,0,100,200);
    }

    @Test public void blocksSensitiveEnglishAndPersianTargets() {
        assertTrue(VisualTapCoordinates.isSensitiveReason("the Send button"));
        assertTrue(VisualTapCoordinates.isSensitiveReason("دکمه پرداخت"));
        assertTrue(VisualTapCoordinates.isSensitiveReason("حذف"));
        assertTrue(VisualTapCoordinates.isSensitiveReason("confirm transfer"));
    }

    @Test public void allowsHarmlessNavigationLabels() {
        assertFalse(VisualTapCoordinates.isSensitiveReason("Open the menu"));
        assertFalse(VisualTapCoordinates.isSensitiveReason("تب تنظیمات"));
    }

    @Test public void rejectsMissingTargetDescription() {
        assertTrue(VisualTapCoordinates.isSensitiveReason(""));
        assertTrue(VisualTapCoordinates.isSensitiveReason(null));
    }
}
