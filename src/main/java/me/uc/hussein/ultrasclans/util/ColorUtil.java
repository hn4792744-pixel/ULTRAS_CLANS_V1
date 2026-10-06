package me.uc.hussein.ultrasclans.util;

import net.md_5.bungee.api.ChatColor;

/**
 * أداة مساعدة لتحويل رموز الألوان (& و hex) إلى نص قابل للعرض.
 * تدعم الصيغتين: &c التقليدية، و &#RRGGBB للألوان السداسية (Hex).
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        String result = translateHex(input);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', result);
    }

    private static String translateHex(String input) {
        StringBuilder builder = new StringBuilder();
        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 7 < length && input.charAt(i + 1) == '#') {
                String hex = input.substring(i + 2, i + 8);
                if (hex.matches("[0-9a-fA-F]{6}")) {
                    builder.append(ChatColor.of("#" + hex));
                    i += 7;
                    continue;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    public static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        return org.bukkit.ChatColor.stripColor(colorize(input));
    }
}
