package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;

public class ReportCategoryListener implements Listener {
    private final BukkitPlugin plugin;

    public ReportCategoryListener(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.toLowerCase().startsWith("/#report-category ")) return;
        message = message.substring(1); // Remove the leading /

        event.setCancelled(true);
        Player sender = event.getPlayer();
        String[] args = message.substring(1).split(" ");
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /report-category <player> <category>");
            return;
        }
        String reportedPlayer = args[1];
        String category = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Call the same report creation logic as before
        if (plugin.getServer().getPlayer(reportedPlayer) == null && plugin.getServer().getOfflinePlayer(reportedPlayer) == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        // Use BukkitReportCommand's createReport logic
        if (plugin.getCommand("report") != null && plugin.getCommand("report").getExecutor() instanceof BukkitReportCommand) {
            ((BukkitReportCommand)plugin.getCommand("report").getExecutor()).createReport(sender, reportedPlayer, category);
        } else {
            sender.sendMessage(ChatColor.RED + "Report system error: cannot process report.");
        }
    }
}
