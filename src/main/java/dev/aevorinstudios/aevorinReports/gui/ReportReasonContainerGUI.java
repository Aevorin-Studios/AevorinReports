package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ReportReasonContainerGUI implements org.bukkit.event.Listener {
    private final BukkitPlugin plugin;
    private final java.util.Map<Player, Integer> playerPages = new java.util.HashMap<>();
    private final java.util.Map<Player, String> playerTargets = new java.util.HashMap<>();

    public ReportReasonContainerGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith("Report: ")) return;
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String targetPlayer = playerTargets.get(player);
        if (targetPlayer == null) return;
        
        // Handle navigation buttons
        if (clickedItem.getType() == Material.ARROW) {
            int currentPage = playerPages.getOrDefault(player, 1);
            if (meta.getDisplayName().contains("Next Page")) {
                showReasonContainerGUI(player, targetPlayer, currentPage + 1);
            } else if (meta.getDisplayName().contains("Previous Page") && currentPage > 1) {
                showReasonContainerGUI(player, targetPlayer, currentPage - 1);
            }
            return;
        }
        
        // Handle report reason selection
        if (clickedItem.getType() == Material.PAPER) {
            String reason = meta.getDisplayName().replace("§c", ""); // Remove color code
            player.closeInventory();
            
            // Submit the report once
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand("report " + targetPlayer + " " + reason);
            });
        }
    }

    public void showReasonContainerGUI(Player player, String targetPlayer) {
        showReasonContainerGUI(player, targetPlayer, 1);
    }

    private void showReasonContainerGUI(Player player, String targetPlayer, int page) {
        // Store player's current page and target
        playerPages.put(player, page);
        playerTargets.put(player, targetPlayer);
        
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

        // Define slots array based on page number
        int[] slots;
        
        // Add book icon and info only on first page
        if (page == 1) {
            ItemStack bookIcon = new ItemStack(Material.BOOK);
            ItemMeta bookMeta = bookIcon.getItemMeta();
            if (bookMeta != null) {
                bookMeta.setDisplayName("§4Reporting: §c" + targetPlayer);
                bookMeta.setLore(List.of(
                    "§7",
                    "§fSelect a reason to report this player",
                    "§fClick on any icon to submit your report",
                    "§7"
                ));
                bookIcon.setItemMeta(bookMeta);
            }
            gui.setItem(13, bookIcon);

            // Define slots for first page (with book)
            slots = new int[] {
                29, 30, 31, 32, 33,  // First row of paper items
                38, 39, 40, 41, 42   // Second row of paper items
            };
        } else {
            // Define slots for subsequent pages (full layout)
            slots = new int[] {
                11, 12, 13, 14, 15,  // First row
                20, 21, 22, 23, 24,  // Second row
                29, 30, 31, 32, 33,  // Third row
                38, 39, 40, 41, 42   // Fourth row
            };
        }

        // Calculate total pages needed based on total slots available
        int itemsPerPage = slots.length;
        int totalPages = (int) Math.ceil((double) reasons.size() / itemsPerPage);
        
        // Calculate start and end indices for current page
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(reasons.size(), startIndex + itemsPerPage);
        
        // Skip if page is out of bounds
        if (startIndex >= reasons.size()) {
            player.closeInventory();
            return;
        }

        // Add report reason items
        for (int i = startIndex; i < endIndex; i++) {
            String reason = reasons.get(i);
            ItemStack item = getReasonItem(reason);
            
            // Make items stand out with enchanted glow
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c" + reason);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                
                meta.setLore(List.of(
                    "§4Target: §c" + targetPlayer,
                    "§aClick to submit report"
                ));
                
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
            }
            
            gui.setItem(slots[i - startIndex], item);
        }

        // Add navigation buttons if there are multiple pages
        if (totalPages > 1) {
            // Next page button (bottom right corner) if not on last page
            if (page < totalPages) {
                ItemStack nextButton = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextButton.getItemMeta();
                if (nextMeta != null) {
                    nextMeta.setDisplayName("§aNext Page ➡");
                    nextMeta.setLore(List.of(
                        "§7Click to go to the next page",
                        String.format("§7Page %d of %d", page, totalPages)
                    ));
                    nextButton.setItemMeta(nextMeta);
                }
                gui.setItem(53, nextButton);
            }
            
            // Previous page button (bottom left corner) if not on first page
            if (page > 1) {
                ItemStack prevButton = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevButton.getItemMeta();
                if (prevMeta != null) {
                    prevMeta.setDisplayName("§a⬅ Previous Page");
                    prevMeta.setLore(List.of(
                        "§7Click to go to the previous page",
                        String.format("§7Page %d of %d", page, totalPages)
                    ));
                    prevButton.setItemMeta(prevMeta);
                }
                gui.setItem(45, prevButton);
            }
        }

        player.openInventory(gui);
    }

    private ItemStack getReasonItem(String reason) {
        return new ItemStack(Material.PAPER);
    }
}

