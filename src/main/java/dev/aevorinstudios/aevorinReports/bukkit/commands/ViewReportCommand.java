package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.model.Report;
import dev.aevorinstudios.aevorinReports.util.PlayerNameResolver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

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

        if (!player.hasPermission("aevorinreports.manage")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

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

        String guiType = plugin.getConfig().getString("reports.gui.type", "book").toLowerCase();
        if (guiType.equals("book")) {
            openBookGUI(player, report);
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

    private void openBookGUI(Player player, Report report) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        if (meta == null) {
            player.sendMessage("§cError creating book interface.");
            return;
        }

        meta.setTitle("Report #" + report.getId());
        meta.setAuthor("Report System");

        String reporterName = report.isAnonymous() ? "Anonymous" : 
            PlayerNameResolver.resolvePlayerName(report.getReporter());
        String reportedName = PlayerNameResolver.resolvePlayerName(report.getReported());
        if (reporterName == null) reporterName = "Unknown";
        if (reportedName == null) reportedName = "Unknown";

        // Create a status indicator based on current status
        String statusColor = switch(report.getStatus()) {
            case PENDING -> "§6";
            case RESOLVED -> "§a";
            case REJECTED -> "§c";
        };

        net.md_5.bungee.api.chat.BaseComponent[] page = new net.md_5.bungee.api.chat.TextComponent[] {
            new net.md_5.bungee.api.chat.TextComponent("§4Report Details \n\n"),
            new net.md_5.bungee.api.chat.TextComponent("§7Reporter: §a" + reporterName + "\n"),
            new net.md_5.bungee.api.chat.TextComponent("§7Reported: §c" + reportedName + "\n"),
            new net.md_5.bungee.api.chat.TextComponent("§7Reason: §c" + report.getCategory() + "\n"),
            new net.md_5.bungee.api.chat.TextComponent("§7Status: " + statusColor + report.getStatus() + "\n"),
            new net.md_5.bungee.api.chat.TextComponent("§7ID: §a" + report.getId() + "\n\n"),
            new net.md_5.bungee.api.chat.TextComponent("§4Click to change status\n")
        };

        // Add status change options with hover text
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            net.md_5.bungee.api.chat.TextComponent pending = new net.md_5.bungee.api.chat.TextComponent("§6⚠ Set as Pending\n");
            pending.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/shiftreport " + report.getId() + " PENDING"
            ));
            pending.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("§7Set this report as pending for review\n§7Current status: §f" + report.getStatus()).create()
            ));
            page = appendComponent(page, pending);
        }

        if (report.getStatus() != Report.ReportStatus.RESOLVED) {
            net.md_5.bungee.api.chat.TextComponent resolved = new net.md_5.bungee.api.chat.TextComponent("§a✔ Mark as Resolved\n");
            resolved.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/shiftreport " + report.getId() + " RESOLVED"
            ));
            resolved.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("§7Mark this report as resolved\n§7Current status: §f" + report.getStatus()).create()
            ));
            page = appendComponent(page, resolved);
        }

        if (report.getStatus() != Report.ReportStatus.REJECTED) {
            net.md_5.bungee.api.chat.TextComponent rejected = new net.md_5.bungee.api.chat.TextComponent("§c✘ Mark as Rejected\n");
            rejected.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/shiftreport " + report.getId() + " REJECTED"
            ));
            rejected.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("§7Reject this report as invalid\n§7Current status: §f" + report.getStatus()).create()
            ));
            page = appendComponent(page, rejected);
        }

        // Add location and evidence if available
        if (report.getWorld() != null) {
            net.md_5.bungee.api.chat.TextComponent location = new net.md_5.bungee.api.chat.TextComponent("\n§7Location: §a" + report.getWorld() + " at " + report.getCoordinates() + "\n");
            page = appendComponent(page, location);
        }

        if (report.getEvidenceData() != null && !report.getEvidenceData().isEmpty()) {
            net.md_5.bungee.api.chat.TextComponent evidence = new net.md_5.bungee.api.chat.TextComponent("§7Evidence: §b" + report.getEvidenceData() + "\n");
            page = appendComponent(page, evidence);
        }

        // Add Back to Categories button
        net.md_5.bungee.api.chat.TextComponent backButton = new net.md_5.bungee.api.chat.TextComponent("\n§7« Back to Categories");
        backButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
            "/reports"
        ));
        backButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
            new net.md_5.bungee.api.chat.ComponentBuilder("§7Return to report categories").create()
        ));
        page = appendComponent(page, backButton);

        meta.spigot().setPages(page);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    private net.md_5.bungee.api.chat.BaseComponent[] appendComponent(net.md_5.bungee.api.chat.BaseComponent[] components, net.md_5.bungee.api.chat.BaseComponent newComponent) {
        net.md_5.bungee.api.chat.BaseComponent[] newArray = new net.md_5.bungee.api.chat.BaseComponent[components.length + 1];
        System.arraycopy(components, 0, newArray, 0, components.length);
        newArray[components.length] = newComponent;
        return newArray;
    }
}