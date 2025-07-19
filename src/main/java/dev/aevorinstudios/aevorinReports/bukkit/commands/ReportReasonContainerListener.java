package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ReportReasonContainerListener implements Listener {
    private final BukkitPlugin plugin;
    
    public ReportReasonContainerListener(BukkitPlugin plugin) {
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
        
        // Extract target and reason from lore
        String targetLine = lore.get(1);
        String reasonLine = lore.get(2);
        
        if (!targetLine.startsWith("§eTarget: ") || !reasonLine.startsWith("§eReason: ")) return;
        
        String targetPlayer = targetLine.substring("§eTarget: §f".length());
        String reason = reasonLine.substring("§eReason: §f".length());
        
        // Close inventory and create the report
        player.closeInventory();
        
        // Submit the report directly
        player.sendMessage("§aSubmitting report against " + targetPlayer + " for " + reason);
        // Create a new instance of BukkitReportCommand to call createReport
        new BukkitReportCommand(plugin).createReport(player, targetPlayer, reason);
    }
}
