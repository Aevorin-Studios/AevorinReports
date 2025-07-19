package dev.aevorinstudios.aevorinReports.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ColorUtils {
    public static TextColor parseColor(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return NamedTextColor.WHITE;
        }

        // Remove the '&' prefix if present
        String code = colorCode.startsWith("&") ? colorCode.substring(1) : colorCode;

        // Map color codes to NamedTextColor
        return switch (code.toLowerCase()) {
            case "0" -> NamedTextColor.BLACK;
            case "1" -> NamedTextColor.DARK_BLUE;
            case "2" -> NamedTextColor.DARK_GREEN;
            case "3" -> NamedTextColor.DARK_AQUA;
            case "4" -> NamedTextColor.DARK_RED;
            case "5" -> NamedTextColor.DARK_PURPLE;
            case "6" -> NamedTextColor.GOLD;
            case "7" -> NamedTextColor.GRAY;
            case "8" -> NamedTextColor.DARK_GRAY;
            case "9" -> NamedTextColor.BLUE;
            case "a" -> NamedTextColor.GREEN;
            case "b" -> NamedTextColor.AQUA;
            case "c" -> NamedTextColor.RED;
            case "d" -> NamedTextColor.LIGHT_PURPLE;
            case "e" -> NamedTextColor.YELLOW;
            case "f" -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
    }
}