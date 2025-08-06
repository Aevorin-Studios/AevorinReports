package dev.aevorinstudios.aevorinReports.commands;

import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HiddenCommandManager {
    private static final Set<String> HIDDEN_COMMANDS = new HashSet<>(Arrays.asList(
            "viewreport",
            "shiftreport"
    ));

    private final JavaPlugin plugin;

    public HiddenCommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerHiddenCommands() {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            for (String command : HIDDEN_COMMANDS) {
                BukkitCommand hiddenCommand = new HiddenCommand(command);
                commandMap.register(plugin.getName().toLowerCase(), hiddenCommand);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register hidden commands: " + e.getMessage());
        }
    }

    private class HiddenCommand extends BukkitCommand {
        public HiddenCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
            // Let the main command handler process it
            return true;
        }
    }

    public static boolean isHiddenCommand(String command) {
        return HIDDEN_COMMANDS.contains(command.toLowerCase());
    }
}
