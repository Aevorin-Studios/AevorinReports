package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.aevorinstudios.aevorinReports.config.ConfigManager.getInstance;

public class BukkitReportsCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();
    private final Map<Report.ReportStatus, List<Report>> reportCache = new HashMap<>();
    private static final long COMMAND_COOLDOWN = 500; // 500 ms cooldown
    private static final long CACHE_DURATION = 5000; // 5 seconds cache duration
    private long lastCacheUpdate = 0;

    public BukkitReportsCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("aevorinreports.manage")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> groups = new ArrayList<>();
            String partialGroup = args[0].toUpperCase();
            
            for (Report.ReportStatus status : Report.ReportStatus.values()) {
                if (status.name().startsWith(partialGroup)) {
                    groups.add(status.name());
                }
            }
            
            return groups;
        }
        
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(sender, getInstance().getMessage("messages.errors.player-only"));
            return true;
        }

        if (player.hasPermission("aevorinreports.manage")) {
            // Admin/Mod view logic continues below
        } else {
            // Regular player view - show their own reports
            new BookGUI(plugin).showPlayerReports(player);
            return true;
        }

        if (args.length == 0) {
            showReportsBook(player);
            return true;
        }

        String group = args[0].toUpperCase();
        Report.ReportStatus status;
        try {
            status = Report.ReportStatus.valueOf(group);
        } catch (IllegalArgumentException e) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, getInstance().getMessage("messages.errors.invalid-group"));
            return true;
        }

        showReportsBookByStatus(player, status);
        return true;
    }

    private void showReportsBook(Player player) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");
        if (guiType.equalsIgnoreCase("container")) {
            showReportsContainerGUI(player);
            return;
        }

        // Check command cooldown
        long currentTime = System.currentTimeMillis();
        long lastTime = lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastTime < COMMAND_COOLDOWN) {
            return;
        }
        lastCommandTime.put(player.getUniqueId(), currentTime);

        // Update cache if needed
        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            reportCache.clear();
            lastCacheUpdate = currentTime;
        }

        new BookGUI(plugin).showReportsBook(player);
    }

    private void showReportsBookByStatus(Player player, Report.ReportStatus status) {
        // Check command cooldown
        long currentTime = System.currentTimeMillis();
        long lastTime = lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastTime < COMMAND_COOLDOWN) {
            return;
        }
        lastCommandTime.put(player.getUniqueId(), currentTime);

        // Update cache if needed
        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            reportCache.clear();
            lastCacheUpdate = currentTime;
        }

        new BookGUI(plugin).showReportsByStatus(player, status);
    }

    private void showReportsContainerGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "Reports Menu");

        // Pending Reports
        ItemStack pending = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta pendingMeta = pending.getItemMeta();
        pendingMeta.setDisplayName("§ePending Reports");
        pendingMeta.setLore(java.util.List.of(
            "§7View all pending reports.",
            "§eClick to view!"
        ));
        pending.setItemMeta(pendingMeta);
        gui.setItem(10, pending);

        // Resolved Reports
        ItemStack resolved = new ItemStack(Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta resolvedMeta = resolved.getItemMeta();
        resolvedMeta.setDisplayName("§aResolved Reports");
        resolvedMeta.setLore(java.util.List.of(
            "§7View all resolved reports.",
            "§eClick to view!"
        ));
        resolved.setItemMeta(resolvedMeta);
        gui.setItem(13, resolved);

        // Rejected Reports
        ItemStack rejected = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta rejectedMeta = rejected.getItemMeta();
        rejectedMeta.setDisplayName("§cRejected Reports");
        rejectedMeta.setLore(java.util.List.of(
            "§7View all rejected reports.",
            "§eClick to view!"
        ));
        rejected.setItemMeta(rejectedMeta);
        gui.setItem(16, rejected);

        player.openInventory(gui);
    }
}