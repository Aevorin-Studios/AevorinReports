package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CategoryContainerGUI {
    private final BukkitPlugin plugin;

    public CategoryContainerGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a paginated category GUI showing reports with the specified status
     * 
     * @param player The player to show the GUI to
     * @param status The status of reports to display
     * @param reports The list of all reports with this status
     * @param page The page number to display (0-based)
     */
    public void openCategoryGUI(Player player, Report.ReportStatus status, List<Report> reports, int page) {
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, status.name().charAt(0) + status.name().substring(1).toLowerCase() + " Reports");
        
        // Fill only the borders with gray stained glass panes
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }
        
        // Fill only the border slots with the background item
        // Top row (0-8)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, background);
        }
        
        // Bottom row (45-53)
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, background);
        }
        
        // Left side (9, 18, 27, 36)
        for (int i = 9; i <= 36; i += 9) {
            gui.setItem(i, background);
        }
        
        // Right side (17, 26, 35, 44)
        for (int i = 17; i <= 44; i += 9) {
            gui.setItem(i, background);
        }

        // Create a list of valid inner slots (excluding only borders)
        java.util.List<Integer> innerSlots = new java.util.ArrayList<>();
        for (int row = 1; row < 5; row++) {  // Rows 1-4 (skip only top and bottom rows)
            for (int col = 1; col < 8; col++) {  // Columns 1-7 (skip left and right column)
                innerSlots.add(row * 9 + col);
            }
        }
        
        // Reserve the center slot in the bottom row for pagination controls
        innerSlots.remove(Integer.valueOf(49)); // Remove slot 49 (reserved for pagination)
        
        // Calculate pagination values
        int reportsPerPage = innerSlots.size(); // Now 28 reports per page with 4 rows
        int totalPages = (int) Math.ceil(reports.size() / (double) reportsPerPage);
        if (totalPages == 0) totalPages = 1; // Always at least one page even if empty
        
        int startIndex = page * reportsPerPage;
        int endIndex = Math.min(startIndex + reportsPerPage, reports.size());
        
        // Add pagination indicator in center slot of navigation row
        if (totalPages > 1) {
            ItemStack pageIndicator = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            pageMeta.setDisplayName("§f§lPage " + (page + 1) + " of " + totalPages);
            pageMeta.setLore(java.util.List.of("§7Showing " + (startIndex + 1) + "-" + endIndex + " of " + reports.size() + " reports"));
            pageIndicator.setItemMeta(pageMeta);
            gui.setItem(49, pageIndicator);
            
            // Previous page arrow (only if we're not on first page)
            if (page > 0) {
                ItemStack prevArrow = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevArrow.getItemMeta();
                prevMeta.setDisplayName("§e§lPrevious Page");
                prevMeta.setLore(java.util.List.of("§7Go to page " + page));
                prevArrow.setItemMeta(prevMeta);
                gui.setItem(48, prevArrow);
            }
            
            // Next page arrow (only if there's a next page)
            if (page < totalPages - 1) {
                ItemStack nextArrow = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextArrow.getItemMeta();
                nextMeta.setDisplayName("§e§lNext Page");
                nextMeta.setLore(java.util.List.of("§7Go to page " + (page + 2)));
                nextArrow.setItemMeta(nextMeta);
                gui.setItem(50, nextArrow);
            }
        }
        
        // Place reports in inner slots only
        for (int i = startIndex; i < endIndex; i++) {
            Report report = reports.get(i);
            int slotIndex = i - startIndex;
            
            // Check if we have slots available
            if (slotIndex >= innerSlots.size()) break;
            
            // Convert UUIDs to player names
            String reporterName = Bukkit.getOfflinePlayer(report.getReporter()).getName();
            String reportedName = Bukkit.getOfflinePlayer(report.getReported()).getName();
            
            if (reporterName == null) reporterName = "Unknown";
            if (reportedName == null) reportedName = "Unknown";
            
            // Use book and quill instead of paper
            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b" + reporterName + " → " + reportedName);
            meta.setLore(java.util.List.of(
                "§8──────────────────",
                "§7Reason: " + report.getReason(),
                "§7ID: " + report.getId(),
                "§8──────────────────",
                "§eClick to manage (read-only)"
            ));
            item.setItemMeta(meta);
            
            // Place the item in the current inner slot
            gui.setItem(innerSlots.get(slotIndex), item);
        }
        player.openInventory(gui);
    }
    
    /**
     * Opens the first page of reports for the given status
     */
    public void openCategoryGUI(Player player, Report.ReportStatus status, List<Report> reports) {
        openCategoryGUI(player, status, reports, 0);
    }
}
