package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ViewReportCommand implements CommandExecutor {
    private final BukkitPlugin plugin;

    public ViewReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        // Permission check moved to after report retrieval to allow self-viewing


        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /viewreport <id>");
            return true;
        }

        try {
            long reportId = Long.parseLong(args[0]);
            showReportDetails(player, reportId);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid report ID!");
        }

        return true;
    }

    private void showReportDetails(Player player, long reportId) {
        Report report = plugin.getDatabaseManager().getReport(reportId);
        if (report == null) {
            player.sendMessage(ChatColor.RED + "Report not found!");
            return;
        }

        // Permission check: Allow if has permission OR is the reporter
        if (!player.hasPermission("aevorinreports.manage") && !report.getReporterUuid().equals(player.getUniqueId())) {
             player.sendMessage(ChatColor.RED + "You don't have permission to view this report!");
             return;
        }

        String guiType = plugin.getConfig().getString("reports.gui.type", "book").toLowerCase();
        if (guiType.equals("book")) {
            new BookGUI(plugin).showReportDetails(player, report);
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, "Manage Report " + report.getId());
        
        // Fill a background with light-gray glass panes
        ItemStack background = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }
        
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Convert UUIDs to player names
        String reporterName = report.isAnonymous() ? "Anonymous" : 
            PlayerNameResolver.resolvePlayerName(report.getReporter());
        String reportedName = PlayerNameResolver.resolvePlayerName(report.getReported());
        if (reporterName == null) reporterName = "Unknown";
        if (reportedName == null) reportedName = "Unknown";
        
        // Info item
        ItemStack info = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§b§lReport Details");
        infoMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        infoMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        infoMeta.setLore(java.util.List.of(
            "§8──────────────────",
            "§7Reporter: §f" + reporterName,
            "§7Reported: §f" + reportedName,
            "§7Reason: §f" + report.getCategory(),
            "§7Status: §f" + report.getStatus(),
            "§7ID: §f" + report.getId(),
            "§8──────────────────"
        ));
        info.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
        info.setItemMeta(infoMeta);
        
        // Center the book in the GUI (slot 22 is the center of a chest inventory)
        gui.setItem(22, info);

        // Add decorative items around the main info for a centered frame
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        
        // Create a visual frame around the centered report details
        for (int slot : new int[]{12, 13, 14, 21, 23, 30, 31, 32}) {
            gui.setItem(slot, glass);
        }
        
        // Move to Pending
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            ItemStack pending = new ItemStack(Material.HOPPER);
            ItemMeta pendingMeta = pending.getItemMeta();
            pendingMeta.setDisplayName("§6§lMove to Pending");
            pendingMeta.setLore(java.util.List.of(
                "§8──────────────────",
                "§7Set this report as pending for review.",
                "§7Current status: §f" + report.getStatus(),
                "§8──────────────────",
                "§6Click to change status"
            ));
            pending.setItemMeta(pendingMeta);
            gui.setItem(45, pending);
        }
        // Move to Resolved
        if (report.getStatus() != Report.ReportStatus.RESOLVED) {
            ItemStack resolved = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta resolvedMeta = resolved.getItemMeta();
            resolvedMeta.setDisplayName("§a§lMove to Resolved");
            resolvedMeta.setLore(java.util.List.of(
                "§8──────────────────",
                "§7Mark this report as resolved.",
                "§7Current status: §f" + report.getStatus(),
                "§8──────────────────",
                "§aClick to change status"
            ));
            resolved.setItemMeta(resolvedMeta);
            gui.setItem(49, resolved);
        }
        // Move to Rejected
        if (report.getStatus() != Report.ReportStatus.REJECTED) {
            ItemStack rejected = new ItemStack(Material.BARRIER);
            ItemMeta rejectedMeta = rejected.getItemMeta();
            rejectedMeta.setDisplayName("§c§lMove to Rejected");
            rejectedMeta.setLore(java.util.List.of(
                "§8──────────────────",
                "§7Reject this report as invalid.",
                "§7Current status: §f" + report.getStatus(),
                "§8──────────────────",
                "§cClick to change status"
            ));
            rejected.setItemMeta(rejectedMeta);
            gui.setItem(53, rejected);
        }
        player.openInventory(gui);
    }
}