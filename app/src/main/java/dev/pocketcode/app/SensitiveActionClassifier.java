package dev.pocketcode.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Conservative native-side signal classifier. Model-provided flags can only add risk, never remove it. */
final class SensitiveActionClassifier {
    private static final Set<String> RISK_TOKENS=new HashSet<>(Arrays.asList(
            "send","submit","publish","post","forward","share",
            "delete","remove","clear","erase","discard","trash",
            "pay","payment","purchase","buy","checkout","transfer","withdraw","deposit","charge","topup",
            "confirm","approve","block","report","logout","unsubscribe","unfollow","archive","mute","ban","kick",
            "revoke","grant","install","uninstall","disable","enable","save","apply","commit","order","book","subscribe",
            "ارسال","بفرست","فرستادن","حذف","پاک","خالی","پرداخت","خرید","انتقال","برداشت","واریز",
            "تایید","تأیید","تراکنش","مسدود","گزارش","خروج","لغو","ذخیره","اعمال","نصب","اجازه",
            "تغییر","ویرایش","فعال","غیرفعال","انتشار","نشر","اشتراک","بازنشر","ثبت","قبول","تسویه","سفارش","رزرو"
    ));

    private SensitiveActionClassifier() { }

    static boolean requiresApproval(String... labels) {
        if(labels==null)return false;
        for(String label:labels){
            if(label==null||label.isEmpty())continue;
            String normalized=label.replaceAll("([a-z])([A-Z])","$1 $2")
                    .toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک')
                    .replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
            if(normalized.isEmpty())continue;
            for(String token:normalized.split("\\s+"))if(RISK_TOKENS.contains(token))return true;
            if(normalized.contains("ارسال")||normalized.contains("پرداخت")
                    ||normalized.contains("حذف")||normalized.contains("برداشت")
                    ||normalized.contains("واریز")||normalized.contains("انتقال"))return true;
        }
        return false;
    }
}
