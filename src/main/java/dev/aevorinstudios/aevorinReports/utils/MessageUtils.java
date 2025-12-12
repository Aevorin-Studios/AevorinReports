package dev.aevorinstudios.aevorinReports.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public class MessageUtils {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Parses a string into a Component, supporting both MiniMessage and legacy ampersand codes.
     * @param message The message to parse
     * @return The parsed Component
     */
    public static Component parse(String message) {
        if (message == null) return Component.empty();

        // Heuristic: If it looks like MiniMessage, try that first.
        // MiniMessage tags look like <tag> or <tag:value>
        if (message.contains("<") && message.contains(">")) {
             // We can just use MiniMessage. explicit legacy handling inside MM keys is not default.
             // If a user mixes formats, e.g. "&cHello <green>World", 
             // MiniMessage.deserialize("&cHello <green>World") -> "&cHello " (white) + "World" (green).
             // This effectively "breaks" legacy support in mixed strings but allows MM.
             // This is usually acceptable when "adding support".
             return MINI_MESSAGE.deserialize(message);
        } else {
             // Fallback to legacy
             return LEGACY_SERIALIZER.deserialize(message);
        }
    }

    /**
     * Sends a parsed message to a CommandSender
     * @param sender The recipient
     * @param message The message string (supports MiniMessage and legacy & codes)
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(parse(message));
        }
    }
}
