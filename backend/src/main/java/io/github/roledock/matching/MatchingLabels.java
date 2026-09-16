package io.github.roledock.matching;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

final class MatchingLabels {
    private MatchingLabels() {}
    private static final Map<String, String> SKILL_ALIASES = Map.of(
            "js", "javascript", "ts", "typescript", "postgres", "postgresql", "springboot", "spring boot");
    private static final Map<String, String> LANGUAGES = Map.of(
            "anglais", "english", "français", "french", "espagnol", "spanish", "allemand", "german");
    private static final java.util.List<String> CEFR = java.util.List.of("a1", "a2", "b1", "b2", "c1", "c2");
    static String text(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Z}]+", " ").strip();
    }
    static String skill(String value) { String key = text(value); return SKILL_ALIASES.getOrDefault(key, key); }
    static String language(String value) { String key = text(value); return LANGUAGES.getOrDefault(key, key); }
    static int level(String value) { return CEFR.indexOf(text(value)); }
    static boolean same(String a, String b) { return !text(a).isEmpty() && text(a).equals(text(b)); }
}
