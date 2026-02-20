package dev.aevorinstudios.aevorinReports.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class to update configuration files while preserving comments and
 * order.
 */
public class ConfigUpdater {

    public static void update(Plugin plugin, String resourceName, File toUpdate) throws IOException {
        if (!toUpdate.exists()) {
            plugin.saveResource(resourceName, false);
            return;
        }

        // Load user config to get their values
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(toUpdate);

        // Load default config from JAR as a template
        InputStream inputStream = plugin.getResource(resourceName);
        if (inputStream == null)
            return;

        List<String> defaultLines = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.toList());

        List<String> newLines = new ArrayList<>();
        Stack<String> pathStack = new Stack<>();
        int lastIndentation = -1;
        boolean skippingUntilNextKey = false;
        int skipLimitIndentation = -1;

        // Strict key pattern: starts with a letter/number and ends with a colon
        // This prevents messages starting with special symbols from being seen as keys.
        String keyPattern = "^[a-zA-Z0-9_-]+:.*";

        for (String line : defaultLines) {
            String trimmed = line.trim();
            int currentIndentation = getIndentation(line);

            // 1. ALWAYS preserve comments and empty lines immediately
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                newLines.add(line);
                continue;
            }

            // 2. Handle block skipping for replaced values
            if (skippingUntilNextKey) {
                // If we hit a new Key at a higher or equal level (less or equal indentation),
                // stop skipping
                if (currentIndentation <= skipLimitIndentation && trimmed.matches(keyPattern)) {
                    skippingUntilNextKey = false;
                } else {
                    // Skip list items, sub-keys, or continuations of the replaced value
                    continue;
                }
            }

            // 3. Detect and Process YAML Keys
            if (trimmed.matches(keyPattern)) {
                int colonIndex = line.indexOf(":");
                String key = trimmed.substring(0, trimmed.indexOf(":")).trim();

                // Path tracking for nested keys
                if (currentIndentation <= lastIndentation) {
                    while (!pathStack.isEmpty() && getPathIndentation(pathStack.size()) >= currentIndentation) {
                        pathStack.pop();
                    }
                }

                pathStack.push(key);
                lastIndentation = currentIndentation;
                String fullPath = String.join(".", pathStack);

                if (userConfig.contains(fullPath) && !userConfig.isConfigurationSection(fullPath)
                        && !key.equals("config-version")) {
                    // Update detected: Inject user value into template structure
                    Object value = userConfig.get(fullPath);
                    String keyPart = line.substring(0, colonIndex + 1);

                    // Improved comment detection: only treat '#' as a comment if preceded by a
                    // space
                    // This prevents placeholders like '#%id%' in messages from being duplicated as
                    // comments.
                    // Improved comment detection: only treat '#' as a comment if it's NOT inside
                    // quotes
                    String commentPart = "";
                    boolean inQuotes = false;
                    for (int i = colonIndex + 1; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == '\"') {
                            inQuotes = !inQuotes;
                        } else if (c == '#' && !inQuotes) {
                            // Found a comment!
                            // Ensure it's preceded by whitespace as per YAML spec
                            if (i > 0 && Character.isWhitespace(line.charAt(i - 1))) {
                                commentPart = " " + line.substring(i).trim();
                                break;
                            }
                        }
                    }

                    if (value instanceof List) {
                        newLines.add(keyPart + commentPart);
                        List<?> list = (List<?>) value;
                        for (Object item : list) {
                            newLines.add(getIndentString(currentIndentation + 2) + "- " + quoteValue(item));
                        }
                    } else {
                        newLines.add(keyPart + " " + quoteValue(value) + commentPart);
                    }

                    // Mark to skip the template's default value block (to prevent duplicates)
                    skippingUntilNextKey = true;
                    skipLimitIndentation = currentIndentation;
                    pathStack.pop();
                } else {
                    // Keep the template line (either a section header or a value the user hasn't
                    // changed)
                    newLines.add(line);
                    // If it's a value node (not a section header), pop it
                    if (!trimmed.endsWith(":")) {
                        pathStack.pop();
                    }
                }
            } else {
                // Not a key, not a comment, not empty - likely a part of a section we are NOT
                // replacing
                newLines.add(line);
            }
        }

        Files.write(toUpdate.toPath(), newLines, StandardCharsets.UTF_8);
    }

    private static int getIndentation(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String getIndentString(int count) {
        return " ".repeat(Math.max(0, count));
    }

    private static int getPathIndentation(int size) {
        return Math.max(0, (size - 1) * 2);
    }

    private static String quoteValue(Object value) {
        if (value == null)
            return "\"\"";
        if (value instanceof String) {
            String s = (String) value;
            if (s.isEmpty())
                return "\"\"";
            // Always quote strings for consistency and safety
            // Replace newlines with \n for valid single-line YAML
            return "\"" + s.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
        return String.valueOf(value);
    }
}
