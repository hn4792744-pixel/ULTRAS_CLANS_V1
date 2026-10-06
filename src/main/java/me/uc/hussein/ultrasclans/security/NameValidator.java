package me.uc.hussein.ultrasclans.security;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

/**
 * يتحقق من صلاحية اسم الكلان وفق القواعد الكاملة في config.yml
 * (clan-creation.name). راجع قسم 6 من المواصفات.
 */
public final class NameValidator {

    public record ValidationResult(boolean valid, String reasonKey) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String reasonKey) {
            return new ValidationResult(false, reasonKey);
        }
    }

    private final ConfigurationSection rules;

    public NameValidator(ConfigurationSection rules) {
        this.rules = rules;
    }

    public ValidationResult validate(String name) {
        if (name == null || name.isBlank()) {
            return ValidationResult.fail("empty");
        }

        int minLength = rules.getInt("min-length", 3);
        int maxLength = rules.getInt("max-length", 16);
        boolean allowArabic = rules.getBoolean("allow-arabic", true);
        boolean allowEnglish = rules.getBoolean("allow-english", true);
        boolean allowNumbers = rules.getBoolean("allow-numbers", true);
        boolean allowSymbols = rules.getBoolean("allow-symbols", false);
        boolean allowSpaces = rules.getBoolean("allow-spaces", false);
        boolean requireLetterFirst = rules.getBoolean("require-letter-first", true);
        List<String> blacklistedWords = rules.getStringList("blacklisted-words");
        List<String> reservedNames = rules.getStringList("reserved-names");

        if (name.length() < minLength) {
            return ValidationResult.fail("too-short");
        }
        if (name.length() > maxLength) {
            return ValidationResult.fail("too-long");
        }
        if (!allowSpaces && name.contains(" ")) {
            return ValidationResult.fail("no-spaces");
        }

        String lower = name.toLowerCase(Locale.ROOT);
        for (String reserved : reservedNames) {
            if (lower.equals(reserved.toLowerCase(Locale.ROOT))) {
                return ValidationResult.fail("reserved");
            }
        }
        for (String blacklisted : blacklistedWords) {
            if (lower.contains(blacklisted.toLowerCase(Locale.ROOT))) {
                return ValidationResult.fail("blacklisted-word");
            }
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            boolean isArabic = isArabicLetter(c);
            boolean isEnglishLetter = Character.isLetter(c) && !isArabic;
            boolean isDigit = Character.isDigit(c);
            boolean isSpace = c == ' ';
            boolean isSymbol = !isArabic && !isEnglishLetter && !isDigit && !isSpace;

            if (isArabic && !allowArabic) {
                return ValidationResult.fail("arabic-not-allowed");
            }
            if (isEnglishLetter && !allowEnglish) {
                return ValidationResult.fail("english-not-allowed");
            }
            if (isDigit && !allowNumbers) {
                return ValidationResult.fail("numbers-not-allowed");
            }
            if (isSymbol && !allowSymbols) {
                return ValidationResult.fail("symbols-not-allowed");
            }

            if (i == 0 && requireLetterFirst && !(isArabic || isEnglishLetter)) {
                return ValidationResult.fail("must-start-with-letter");
            }
        }

        return ValidationResult.ok();
    }

    private boolean isArabicLetter(char c) {
        return c >= 0x0600 && c <= 0x06FF;
    }
}
