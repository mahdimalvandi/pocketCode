package dev.pocketcode.app;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalizes Android recognition results before matching the configured wake phrase. */
final class WakePhraseMatcher {
    private static final Pattern ENGLISH=Pattern.compile("(?<![\\p{L}\\p{N}])pocket\\s*code(?![\\p{L}\\p{N}])|(?<![\\p{L}\\p{N}])pocketcode(?![\\p{L}\\p{N}])");
    private static final Pattern PERSIAN=Pattern.compile("(?<![\\p{L}\\p{N}])پاکت\\s*کد(?![\\p{L}\\p{N}])|(?<![\\p{L}\\p{N}])پاکتکد(?![\\p{L}\\p{N}])");
    private static final Pattern PERSIAN_VARIANT=Pattern.compile("(?<![\\p{L}\\p{N}])پاکت\\s*کود(?![\\p{L}\\p{N}])|(?<![\\p{L}\\p{N}])پاکتکود(?![\\p{L}\\p{N}])");

    private WakePhraseMatcher() { }

    static boolean matches(String phrase) {
        if(phrase==null)return false;
        String normalized=phrase.toLowerCase(Locale.ROOT)
                .replace('ي','ی')
                .replace('ك','ک')
                .replaceAll("[^\\p{L}\\p{N}]+"," ")
                .trim().replaceAll("\\s+"," ");
        return ENGLISH.matcher(normalized).find()
                ||PERSIAN.matcher(normalized).find()
                ||PERSIAN_VARIANT.matcher(normalized).find();
    }
}
