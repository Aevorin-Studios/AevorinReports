package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ReportReasonContainerGUI {
    private final BukkitPlugin plugin;

    public ReportReasonContainerGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    public void showReasonContainerGUI(Player player, String targetPlayer) {
        // Get report reasons from config
        List<String> reasons = plugin.getConfig().getStringList("reports.categories");
        if (reasons.isEmpty()) {
            reasons = new ArrayList<>(List.of(
                "Hacking/Cheating", 
                "Harassment/Bullying", 
                "Inappropriate Language", 
                "Spam/Advertisement", 
                "Griefing/Vandalism", 
                "Exploiting Bugs", 
                "Inappropriate Skin/Name"
            ));
        }
        
        // Filter out "Other" reason
        reasons = reasons.stream()
                .filter(reason -> !reason.equalsIgnoreCase("Other"))
                .collect(java.util.stream.Collectors.toList());

        // Always use 54 slots (6 rows) for better aesthetics
        int size = 54;
        Inventory gui = Bukkit.createInventory(null, size, "Report: " + targetPlayer);
        
        // Fill with light gray glass panes as background
        ItemStack background = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }
        
        for (int i = 0; i < size; i++) {
            gui.setItem(i, background);
        }
        
        // Add report info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lReporting: §b" + targetPlayer);
            infoMeta.setLore(List.of(
                "§7",
                "§fSelect a reason to report this player",
                "§fClick on any icon to submit your report",
                "§7"
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(13, info);

        // Calculate center positions for a nice layout
        int[] slots;
        if (reasons.size() <= 7) {
            // Single row centered layout for 7 or fewer reasons (shifted left and down)
            slots = new int[]{28, 29, 30, 31, 32, 33, 34};
        } else {
            // Two row layout for more reasons (shifted left and down)
            slots = new int[]{
                27, 28, 29, 30, 31, 32, 33,  // Top row
                36, 37, 38, 39, 40, 41, 42   // Bottom row
            };
        }
        
        // Add reason items
        for (int i = 0; i < Math.min(reasons.size(), slots.length); i++) {
            String reason = reasons.get(i);
            ItemStack item = getReasonItem(reason);
            
            // Make items stand out with enchanted glow
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b§l" + reason);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                
                meta.setLore(List.of(
                    "§8──────────────────",
                    "§eTarget: §f" + targetPlayer,
                    "§eReason: §f" + reason,
                    "§8──────────────────",
                    "§aClick to submit report"
                ));
                
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
            }
            
            gui.setItem(slots[i], item);
        }

        player.openInventory(gui);
    }

    private ItemStack getReasonItem(String reason) {
        // Return different items based on reason
        return switch (reason.toLowerCase()) {
            case "hacking/cheating" -> new ItemStack(Material.COMPASS);
            case "harassment/bullying" -> new ItemStack(Material.PAPER);
            case "inappropriate language" -> new ItemStack(Material.BOOK);
            case "spam/advertisement" -> new ItemStack(Material.MAP);
            case "griefing/vandalism" -> new ItemStack(Material.TNT);
            case "exploiting bugs" -> new ItemStack(Material.SPIDER_EYE);
            case "inappropriate skin/name" -> new ItemStack(Material.NAME_TAG);
            default -> new ItemStack(Material.WRITABLE_BOOK);
        };
    }
}
