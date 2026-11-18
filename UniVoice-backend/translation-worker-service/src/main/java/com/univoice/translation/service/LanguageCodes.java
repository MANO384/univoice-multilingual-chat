package com.univoice.translation.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LanguageCodes {

    private static final Map<String, String> SARVAM_LOCALES = Map.ofEntries(
            Map.entry("hi", "hi-IN"),
            Map.entry("ta", "ta-IN"),
            Map.entry("te", "te-IN"),
            Map.entry("kn", "kn-IN"),
            Map.entry("ml", "ml-IN"),
            Map.entry("bn", "bn-IN"),
            Map.entry("mr", "mr-IN"),
            Map.entry("gu", "gu-IN"),
            Map.entry("pa", "pa-IN"),
            Map.entry("od", "od-IN"),
            Map.entry("en", "en-IN")
    );

    private static final Set<String> INDIC_LANGUAGES = Set.of(
            "hi", "ta", "te", "kn", "ml", "bn", "mr", "gu", "pa", "od"
    );

    private LanguageCodes() {}

    public static String base(String code) {
        if (code == null || code.isBlank()) return "auto";
        int split = Math.max(code.indexOf('-'), code.indexOf('_'));
        String base = split == -1 ? code : code.substring(0, split);
        return base.trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<String> toSarvam(String baseLang) {
        if (baseLang == null) return Optional.empty();
        return Optional.ofNullable(SARVAM_LOCALES.get(baseLang.toLowerCase(Locale.ROOT)));
    }

    public static boolean isSarvamSupported(String baseLang) {
        return baseLang != null && SARVAM_LOCALES.containsKey(baseLang.toLowerCase(Locale.ROOT));
    }

    public static boolean isIndic(String baseLang) {
        return baseLang != null && INDIC_LANGUAGES.contains(baseLang.toLowerCase(Locale.ROOT));
    }
}