package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all GUI-related functionality for the AevorinReports plugin.
 * This class centralizes the creation and display of both book and chest GUIs for report management.
 */
public class BookGUI {
    private final BukkitPlugin plugin;
    private static final int ITEMS_PER_PAGE = 8;

    public BookGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Shows the report categories book GUI to a player.
     * @param player The player to show the GUI to
     * @param targetPlayer The player being reported
     */
    public void showReportCategories(Player player, String targetPlayer) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        meta.setTitle("Report Categories");
        meta.setAuthor("Server");

        List<String> categories = plugin.getConfig().getStringList("reports.categories");
        List<BaseComponent[]> pages = new ArrayList<>();
        
        // First page with headers
        ComponentBuilder currentPage = new ComponentBuilder("§4Reporting Player:\n");
        currentPage.append("§c" + targetPlayer + "\n\n");
        currentPage.append("§4Reasons:\n");
        int itemsOnPage = 0;

        for (String category : categories) {
            if (itemsOnPage >= ITEMS_PER_PAGE) {
                pages.add(currentPage.create());
                // Subsequent pages without headers
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            TextComponent categoryText = new TextComponent("§c• " + category);
            categoryText.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/report " + targetPlayer + " " + category
            ));
            categoryText.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to report for: §f" + category).create()
            ));
            currentPage.append(categoryText);
            
            if (itemsOnPage < ITEMS_PER_PAGE - 1) {
                currentPage.append("\n");
            }
            itemsOnPage++;
        }

        if (itemsOnPage > 0) {
            pages.add(currentPage.create());
        }

        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows the reports book GUI to a player.
     * @param player The player to show the GUI to
     */
    public void showReportsBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        meta.setTitle("Reports Category");
        meta.setAuthor("Server");

        ComponentBuilder page = new ComponentBuilder("§4Reports Category\n\n");

        // Pending Reports Button
        TextComponent pendingButton = new TextComponent("§6• Pending Reports");
        pendingButton.setClickEvent(new ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            "/reports pending"
        ));
        pendingButton.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Click to view pending reports").create()
        ));
        page.append(pendingButton).append("\n\n");

        // Resolved Reports Button
        TextComponent resolvedButton = new TextComponent("§a• Resolved Reports");
        resolvedButton.setClickEvent(new ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            "/reports resolved"
        ));
        resolvedButton.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Click to view resolved reports").create()
        ));
        page.append(resolvedButton).append("\n\n");

        // Rejected Reports Button
        TextComponent rejectedButton = new TextComponent("§c• Rejected Reports");
        rejectedButton.setClickEvent(new ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            "/reports rejected"
        ));
        rejectedButton.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Click to view rejected reports").create()
        ));
        page.append(rejectedButton);

        meta.spigot().setPages(page.create());
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows reports filtered by status in a book GUI.
     * @param player The player to show the GUI to
     * @param status The status to filter reports by
     */
    public void showReportsByStatus(Player player, Report.ReportStatus status) {
        List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(status);
        if (reports.isEmpty()) {
            player.sendMessage("§cNo " + status.toString().toLowerCase() + " reports found.");
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        meta.setTitle(status + " Reports");
        meta.setAuthor("Server");

        List<BaseComponent[]> pages = new ArrayList<>();
        int itemsOnPage = 0;
        int reportNumber = 1;

        // First page with header and total count
        ComponentBuilder currentPage = new ComponentBuilder("§4" + status + " Reports\n");
        currentPage.append("§7Total: " + reports.size() + "\n\n");

        for (Report report : reports) {
            if (itemsOnPage >= ITEMS_PER_PAGE - 1) { // Reserve space for back button
                // Add back button to current page
                currentPage.append("\n\n");
                TextComponent backButton = new TextComponent("§8« Back to Categories");
                backButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/reports"
                ));
                backButton.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Click to go back to report categories").create()
                ));
                currentPage.append(backButton);
                
                // Add the page and start a new one
                pages.add(currentPage.create());
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            String reportedName = PlayerNameResolver.resolvePlayerName(report.getReported());
            
            TextComponent reportEntry = new TextComponent("§4" + reportNumber + ". §c" + reportedName);
            reportEntry.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/viewreport " + report.getId()
            ));
            reportEntry.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to view details").create()
            ));
            currentPage.append(reportEntry).append("\n");
            
            itemsOnPage++;
            reportNumber++;
        }

        // Add "Back to Categories" button to the last page
        if (itemsOnPage > 0) {
            currentPage.append("\n\n");
            TextComponent backButton = new TextComponent("§8« Back to Categories");
            backButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/reports"
            ));
            backButton.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to go back to report categories").create()
            ));
            currentPage.append(backButton);
            pages.add(currentPage.create());
        }

        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows reports submitted by the player in a book GUI.
     * @param player The player to show the GUI to
     */
    public void showPlayerReports(Player player) {
        List<Report> reports = plugin.getDatabaseManager().getReportsByReporter(player.getUniqueId());
        if (reports.isEmpty()) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, "<red>You have not submitted any reports.");
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        meta.setTitle("Your Reports");
        meta.setAuthor("Server");

        List<BaseComponent[]> pages = new ArrayList<>();
        int itemsOnPage = 0;
        int reportNumber = 1;

        // First page with header and total count
        ComponentBuilder currentPage = new ComponentBuilder("§4Your Reports\n");
        currentPage.append("§7Total: " + reports.size() + "\n\n");

        for (Report report : reports) {
            if (itemsOnPage >= ITEMS_PER_PAGE - 1) { // Reserve space for close/back button
                pages.add(currentPage.create());
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            String reportedName = PlayerNameResolver.resolvePlayerName(report.getReported());
            String statusColor = switch(report.getStatus()) {
                case PENDING -> "§6";
                case RESOLVED -> "§a";
                case REJECTED -> "§c";
                default -> "§7";
            };
            
            TextComponent reportEntry = new TextComponent("§4" + reportNumber + ". §c" + reportedName + " " + statusColor + "(" + report.getStatus() + ")");
            reportEntry.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/viewreport " + report.getId()
            ));
            reportEntry.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to view details").create()
            ));
            currentPage.append(reportEntry).append("\n");
            
            itemsOnPage++;
            reportNumber++;
        }

        if (itemsOnPage > 0) {
            pages.add(currentPage.create());
        }

        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows detailed report information in a GUI.
     * @param player The player to show the GUI to
     * @param report The report to show details for
     */
    public void showReportDetails(Player player, Report report) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book").toLowerCase();
        if (guiType.equals("book")) {
            showReportDetailsBook(player, report);
        } else {
            showReportDetailsChest(player, report);
        }
    }

    /**
     * Shows detailed report information in a book GUI.
     * @param player The player to show the GUI to
     * @param report The report to show details for
     */
    private void showReportDetailsBook(Player player, Report report) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        meta.setTitle("Report Details");
        meta.setAuthor("Server");

        ComponentBuilder page = new ComponentBuilder("§cReport Details\n\n");

        // Convert UUIDs to player names
        String reporterName = report.isAnonymous() ? "Anonymous" : 
            PlayerNameResolver.resolvePlayerName(report.getReporter());
        String reportedName = PlayerNameResolver.resolvePlayerName(report.getReported());
        
        // Add report information
        page.append("§7Reporter: §a" + reporterName + "\n");
        page.append("§7Reported: §c" + reportedName + "\n");
        page.append("§7Reason: §7" + report.getCategory() + "\n");
        page.append("§7Status: §c" + report.getStatus() + "\n");
        page.append("§7ID: §0" + report.getId() + "\n\n");

        // Add status change buttons ONLY if player has permission
        if (player.hasPermission("aevorinreports.manage")) {
            // Add "Click to change status" text
            page.append("§8Click to change status\n");

            if (report.getStatus() == Report.ReportStatus.PENDING) {
                // Resolve button
                TextComponent resolveButton = new TextComponent("§a✔ Mark as Resolved\n");
                resolveButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/setreportstatus " + report.getId() + " to RESOLVED"
                ));
                resolveButton.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Click to mark as resolved").create()
                ));
                page.append(resolveButton);

                // Reject button
                TextComponent rejectButton = new TextComponent("§c✘ Mark as Rejected\n");
                rejectButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/setreportstatus " + report.getId() + " to REJECTED"
                ));
                rejectButton.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Click to mark as rejected").create()
                ));
                page.append(rejectButton);
            } else {
                // Return to pending button
                TextComponent pendingButton = new TextComponent("§6⚠ Set as Pending\n");
                pendingButton.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/setreportstatus " + report.getId() + " to PENDING"
                ));
                pendingButton.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Click to return to pending").create()
                ));
                page.append(pendingButton);
            }
        }

        // Add "Back to Full List" button with current status
        TextComponent backButton = new TextComponent("\n§8« Back to Full List");
        backButton.setClickEvent(new ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            "/reports " + report.getStatus().toString().toLowerCase()
        ));
        backButton.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("§7Return to report categories").create()
        ));
        page.append(backButton);

        meta.spigot().setPages(page.create());
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows detailed report information in a chest GUI.
     * @param player The player to show the GUI to
     * @param report The report to show details for
     */
    private void showReportDetailsChest(Player player, Report report) {
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
        infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        infoMeta.setLore(List.of(
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
        
        if (player.hasPermission("aevorinreports.manage")) {
            // Move to Pending
            if (report.getStatus() != Report.ReportStatus.PENDING) {
                ItemStack pending = new ItemStack(Material.HOPPER);
                ItemMeta pendingMeta = pending.getItemMeta();
                pendingMeta.setDisplayName("§6§lMove to Pending");
                pendingMeta.setLore(List.of(
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
                resolvedMeta.setLore(List.of(
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
                rejectedMeta.setLore(List.of(
                    "§8──────────────────",
                    "§7Reject this report as invalid.",
                    "§7Current status: §f" + report.getStatus(),
                    "§8──────────────────",
                    "§cClick to change status"
                ));
                rejected.setItemMeta(rejectedMeta);
                gui.setItem(53, rejected);
            }
        }
        player.openInventory(gui);
    }
}