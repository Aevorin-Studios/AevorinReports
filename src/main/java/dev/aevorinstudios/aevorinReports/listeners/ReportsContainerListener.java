package dev.aevorinstudios.aevorinReports.listeners;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.gui.CategoryContainerGUI;
import dev.aevorinstudios.aevorinReports.gui.ReportManageGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ReportsContainerListener implements Listener {
    private final BukkitPlugin plugin;
    public ReportsContainerListener(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        if (!(title.equals("Reports Menu") || title.endsWith("Reports") || title.startsWith("Manage Report "))) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String display = meta.getDisplayName();
        if (display == null) {
            event.setCancelled(true);
            return;
        }

        // Handle category selection
        if (display.contains("Pending Reports")) {
            event.setCancelled(true);
            List<dev.aevorinstudios.aevorinReports.reports.Report> reports = plugin.getDatabaseManager().getReportsByStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.PENDING);
            new CategoryContainerGUI(plugin).openCategoryGUI(player, dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.PENDING, reports);
            return;
        } else if (display.contains("Resolved Reports")) {
            event.setCancelled(true);
            List<dev.aevorinstudios.aevorinReports.reports.Report> reports = plugin.getDatabaseManager().getReportsByStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.RESOLVED);
            new CategoryContainerGUI(plugin).openCategoryGUI(player, dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.RESOLVED, reports);
            return;
        } else if (display.contains("Rejected Reports")) {
            event.setCancelled(true);
            List<dev.aevorinstudios.aevorinReports.reports.Report> reports = plugin.getDatabaseManager().getReportsByStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.REJECTED);
            new CategoryContainerGUI(plugin).openCategoryGUI(player, dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.REJECTED, reports);
            return;
        }

        // If we're in a category GUI (Pending/Resolved/Rejected Reports)
        if (title.endsWith("Reports")) {
            event.setCancelled(true);
            
            // Handle pagination arrows
            if (event.getSlot() == 48 || event.getSlot() == 50) {
                // Make sure we have an arrow and metadata
                if (clicked.getType() != Material.ARROW) return;
                List<String> arrowLore = meta.getLore();
                if (arrowLore == null || arrowLore.isEmpty()) return;
                
                // Extract the status from the title
                String statusStr = title.substring(0, title.indexOf(" ")).toUpperCase();
                Report.ReportStatus status;
                try {
                    status = Report.ReportStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    return; // Invalid status
                }
                
                // Extract the page number
                String pageInfo = arrowLore.get(0);
                if (!pageInfo.startsWith("§7Go to page ")) return;
                try {
                    int targetPage = Integer.parseInt(pageInfo.replace("§7Go to page ", "").trim()) - 1; // Convert to 0-based
                    if (targetPage < 0) return;
                    
                    // Get reports for this status and open the targeted page
                    List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(status);
                    new CategoryContainerGUI(plugin).openCategoryGUI(player, status, reports, targetPage);
                } catch (NumberFormatException ignored) {}
                
                return;
            }
            
            // Handle report item clicks
            if (event.getSlot() < 0 || event.getSlot() >= inv.getSize()) return;
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.startsWith("§7ID: ")) {
                        try {
                            long reportId = Long.parseLong(line.replace("§7ID: ", "").trim());
                            Report report = plugin.getDatabaseManager().getReport(reportId);
                            if (report != null) {
                                player.sendMessage("§7Opening management for report ID: " + reportId);
                                new ReportManageGUI(plugin).open(player, report);
                            }
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                }
            }
            return;
        }
        // If we're in the management GUI for a report
        if (title.startsWith("Manage Report ")) {
            event.setCancelled(true);
            int slot = event.getSlot();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            ItemMeta clickedMeta = clickedItem.getItemMeta();
            String clickedName = clickedMeta.getDisplayName();
            // Only allow move actions on slots 45, 49, 53
            if (slot != 45 && slot != 49 && slot != 53) return;
            try {
                long reportId = Long.parseLong(title.replace("Manage Report ", "").trim());
                dev.aevorinstudios.aevorinReports.reports.Report report = plugin.getDatabaseManager().getReport(reportId);
                if (report == null) return;
                if (clickedName == null) return;
                if (slot == 45 && clickedName.contains("Move to Pending")) {
                    report.setStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.PENDING);
                    plugin.getDatabaseManager().updateReport(report);
                    player.sendMessage("§eReport moved to Pending.");
                } else if (slot == 49 && clickedName.contains("Move to Resolved")) {
                    report.setStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.RESOLVED);
                    plugin.getDatabaseManager().updateReport(report);
                    player.sendMessage("§aReport moved to Resolved.");
                } else if (slot == 53 && clickedName.contains("Move to Rejected")) {
                    report.setStatus(dev.aevorinstudios.aevorinReports.reports.Report.ReportStatus.REJECTED);
                    plugin.getDatabaseManager().updateReport(report);
                    player.sendMessage("§cReport moved to Rejected.");
                } else {
                    return;
                }
                // After move, reopen the new category GUI
                java.util.List<dev.aevorinstudios.aevorinReports.reports.Report> updatedReports = plugin.getDatabaseManager().getReportsByStatus(report.getStatus());
                new CategoryContainerGUI(plugin).openCategoryGUI(player, report.getStatus(), updatedReports);
            } catch (Exception ignored) {}
            return;
        }
        if (title.endsWith("Reports")) {
            // Try to extract report id from lore
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.startsWith("§7ID: ")) {
                        try {
                            long reportId = Long.parseLong(line.replace("§7ID: ", "").trim());
                            Report report = plugin.getDatabaseManager().getReport(reportId);
                            if (report != null) {
                                new ReportManageGUI(plugin).open(player, report);
                            }
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                }
            }
            event.setCancelled(true);
        }
    }
}
