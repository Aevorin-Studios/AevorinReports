package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ReportCategoryContainerListener implements Listener {
    private final BukkitPlugin plugin;
    
    public ReportCategoryContainerListener(BukkitPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (!title.startsWith("Report: ")) return;
        
        event.setCancelled(true); // Prevent picking up items
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 4) return;
        
        // Extract target and category from lore
        String targetLine = lore.get(3);
        String categoryLine = lore.get(2);
        
        if (!targetLine.startsWith("§7Target: ") || !categoryLine.startsWith("§7Category: ")) return;
        
        String targetPlayer = targetLine.substring("§7Target: ".length());
        String category = categoryLine.substring("§7Category: ".length());
        
        // Close inventory and create the report
        player.closeInventory();
        
        // Submit the report directly
        player.sendMessage("§aSubmitting report against " + targetPlayer + " for " + category);
        // Create a new instance of BukkitReportCommand to call createReport
        new BukkitReportCommand(plugin).createReport(player, targetPlayer, category);
    }
}
