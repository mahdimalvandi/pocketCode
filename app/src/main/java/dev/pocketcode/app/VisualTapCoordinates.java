package dev.pocketcode.app;

import java.util.Locale;

/** Pure coordinate mapping and conservative label checks for one-shot screenshot-based taps. */
final class VisualTapCoordinates {
    private VisualTapCoordinates() { }

    /** Maps image pixels to screen coordinates while restricting taps to the active app window. */
    static float[] map(double imageX, double imageY, int imageWidth, int imageHeight,
                       int mapLeft, int mapTop, int mapRight, int mapBottom,
                       int allowedLeft, int allowedTop, int allowedRight, int allowedBottom) {
        if (!Double.isFinite(imageX) || !Double.isFinite(imageY)
                || imageWidth <= 0 || imageHeight <= 0
                || imageX < 0 || imageY < 0 || imageX >= imageWidth || imageY >= imageHeight
                || mapRight <= mapLeft || mapBottom <= mapTop
                || allowedRight <= allowedLeft || allowedBottom <= allowedTop) {
            throw new IllegalArgumentException("Invalid screenshot coordinate space.");
        }
        float screenX = mapLeft + (float) ((imageX + 0.5d) * (mapRight - mapLeft) / imageWidth);
        float screenY = mapTop + (float) ((imageY + 0.5d) * (mapBottom - mapTop) / imageHeight);
        int pointX = Math.round(screenX);
        int pointY = Math.round(screenY);
        if (pointX < allowedLeft || pointX >= allowedRight
                || pointY < allowedTop || pointY >= allowedBottom) {
            throw new IllegalArgumentException("Tap point is outside the active app window.");
        }
        return new float[]{screenX, screenY};
    }

    /** Heuristic deny-list for controls whose labels imply consequential or sensitive actions. */
    static boolean isSensitiveReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) return true;
        String normalized = reason.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
        return normalized.matches("(?s).*(send|submit|delete|remove|clear|pay|payment|purchase|buy|transfer|withdraw|deposit|confirm|logout|signout|block|report|permission|install|approve|authorize|publish|post|forward|call|share|ارسال|حذف|پاک|پرداخت|خرید|انتقال|برداشت|واریز|اشتراک|ثبت|تراکنش|تأیید|تایید|مسدود|گزارش|خروج|نصب|انتشار).*");
    }
}
