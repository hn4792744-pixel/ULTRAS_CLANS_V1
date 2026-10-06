package me.uc.hussein.ultrasclans.util;

/**
 * يحول عدد الثواني إلى نص مقروء (مثال: 1h 5m 3s / يوم واحد و5 دقائق).
 * يبقى بسيطًا ومحايدًا للغة، الترجمة الكاملة تُضاف لاحقًا عبر messages.
 */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    public static String format(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 || builder.length() == 0) builder.append(seconds).append("s");
        return builder.toString().trim();
    }
}
