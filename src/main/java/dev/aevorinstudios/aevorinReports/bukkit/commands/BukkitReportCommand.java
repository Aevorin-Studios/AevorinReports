package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;
import net.md_5.bungee.api.ChatColor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BukkitReportCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;

    public BukkitReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("aevorinreports.report")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /report <player> [reason]");
            return true;
        }

        String targetPlayer = args[0];
        Player target = plugin.getServer().getPlayer(targetPlayer);
        
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        // Self-reporting check
        boolean allowSelfReporting = plugin.getConfig().getBoolean("reports.allow-self-reporting", true);
        if (!allowSelfReporting && player.getName().equalsIgnoreCase(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "You cannot report yourself.");
            return true;
        }

        // If reason is provided, create report directly
        if (args.length > 1) {
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            
            // Handle custom reason
            if (reason.equalsIgnoreCase("custom")) {
                if (!plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                    player.sendMessage(ChatColor.RED + plugin.getConfig().getString("messages.custom-reason-disabled", "Custom reasons are disabled."));
                    return true;
                }
                
                // Store player and target info for custom reason handling
                plugin.getCustomReasonHandler().startCustomReason(player, targetPlayer);
                player.sendMessage(ChatColor.GREEN + plugin.getConfig().getString("messages.enter-custom-reason", "Please enter your reason in the chat:"));
                return true;
            }
            
            createReport(player, targetPlayer, reason);
            return true;
        }

        // If no reason provided, show GUI
        showReportCategories(player, target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("aevorinreports.report")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            String partialName = args[0].toLowerCase();
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    playerNames.add(player.getName());
                }
            }
            
            return playerNames;
        } else if (args.length == 2) {
            List<String> suggestions = new ArrayList<>(plugin.getConfig().getStringList("reports.categories"));
            if (plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                suggestions.add("custom");
            }
            
            String partialReason = args[1].toLowerCase();
            return suggestions.stream()
                .filter(reason -> reason.toLowerCase().startsWith(partialReason))
                .toList();
        }
        
        return new ArrayList<>();
    }

    public void createReport(Player reporter, String targetPlayer, String category) {
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reporterUuid(reporter.getUniqueId())
                .reportedUuid(plugin.getServer().getOfflinePlayer(targetPlayer).getUniqueId())
                .reason(category)
                .serverName(plugin.getServer().getName())
                .status(Report.ReportStatus.PENDING)
                .isAnonymous(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Save report to database
        DatabaseManager.getInstance().saveReport(report);
        // The report ID is set by the saveReport method

        // Notify staff members with permission
        for (Player staff : plugin.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("aevorinreports.notify")) {
                staff.sendMessage(ChatColor.RED + "[Reports] " + 
                    ChatColor.RED + reporter.getName() + 
                    ChatColor.WHITE + " has reported " + 
                    ChatColor.YELLOW + targetPlayer + 
                    ChatColor.WHITE + " for: " + 
                    ChatColor.YELLOW + category);
            }
        }

        // Send report to proxy if proxy sync is enabled and authenticated
        if (plugin.getConfig().getBoolean("proxy.enabled", false) && 
            plugin.getTokenSyncManager() != null && 
            plugin.getTokenSyncManager().isAuthenticated()) {
            
            // Prepare report data for sync
            Map<String, String> reportData = new HashMap<>();
            reportData.put("id", String.valueOf(report.getId()));
            reportData.put("reporter_uuid", report.getReporterUuid().toString());
            reportData.put("reporter_name", reporter.getName());
            reportData.put("reported_uuid", report.getReportedUuid().toString());
            reportData.put("reported_name", targetPlayer);
            reportData.put("reason", report.getReason());
            reportData.put("server_name", report.getServerName());
            reportData.put("status", report.getStatus().toString());
            reportData.put("created_at", report.getCreatedAt().toString());
            
            // Send report to proxy
            plugin.getTokenSyncManager().sendMessage("NEW_REPORT", reportData)
                .thenAccept(success -> {
                    if (success) {
                        plugin.getLogger().info("Report #" + report.getId() + " successfully synced with proxy");
                    } else {
                        plugin.getLogger().warning("Failed to sync report #" + report.getId() + " with proxy");
                    }
                });
        }

        reporter.sendMessage(ChatColor.GREEN + "Your report has been submitted successfully!");
    }

    private void showReportCategories(Player player, String targetPlayer) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");
        
        if (guiType.equalsIgnoreCase("container")) {
            // Use container GUI
            new ReportReasonContainerGUI(plugin).showReasonContainerGUI(player, targetPlayer);
        } else {
            // Default to book GUI
            showReportBookGUI(player, targetPlayer);
        }
    }
    
    private void showReportBookGUI(Player player, String targetPlayer) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) return;

        meta.setTitle(ChatColor.GOLD + "Report Player");
        meta.setAuthor("Server Admin");

        List<String> categories = plugin.getConfig().getStringList("reports.categories");
        if (categories.isEmpty()) {
            categories = new ArrayList<>(List.of("Hacking/Cheating", "Harassment/Bullying", "Inappropriate Language", "Spam/Advertisement", "Griefing/Vandalism", "Other"));
        }

        List<net.md_5.bungee.api.chat.BaseComponent[]> pages = new ArrayList<>();
        net.md_5.bungee.api.chat.ComponentBuilder currentBuilder = new net.md_5.bungee.api.chat.ComponentBuilder();

        // Add header
        currentBuilder.append(ChatColor.DARK_RED + "Reporting player:\n")
                     .append(ChatColor.RED + targetPlayer + "\n\n")
                     .append(ChatColor.DARK_RED + "Select a reason:\n" + ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n");

        // Use direct command for confirmation blocking
        String commandPrefix = "/report-category";

        // Calculate pages
        int headerLines = 5;
        int maxLinesPerPage = 12;
        int firstPageCategories = maxLinesPerPage - headerLines - 2;
        int subsequentPageCategories = maxLinesPerPage - 3 - 2;
        
        int totalPages = 1;
        int remainingCategories = categories.size();
        if (remainingCategories > firstPageCategories) {
            remainingCategories -= firstPageCategories;
            totalPages += (int)Math.ceil((double)remainingCategories / subsequentPageCategories);
        }

        int currentPageCategories = 0;
        boolean isFirstPage = true;
        int currentPage = 1;

        // Add all regular categories
        for (String category : categories) {
            int maxCategoriesForCurrentPage = isFirstPage ? firstPageCategories : subsequentPageCategories;
            
            if (currentPageCategories >= maxCategoriesForCurrentPage) {
                currentBuilder.append("\n" + ChatColor.GRAY + "Page " + currentPage + " of " + totalPages);
                pages.add(currentBuilder.create());
                
                currentBuilder = new net.md_5.bungee.api.chat.ComponentBuilder()
                    .append(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n");
                
                currentPageCategories = 0;
                isFirstPage = false;
                currentPage++;
            }

            net.md_5.bungee.api.chat.TextComponent categoryComponent = new net.md_5.bungee.api.chat.TextComponent();
            categoryComponent.addExtra(new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "• "));
            
            net.md_5.bungee.api.chat.TextComponent categoryText = new net.md_5.bungee.api.chat.TextComponent(category);
            categoryText.setColor(net.md_5.bungee.api.ChatColor.RED);
            categoryText.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/report " + targetPlayer + " " + category
            ));
            categoryText.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("Click to report for " + category).create()
            ));
            
            categoryComponent.addExtra(categoryText);
            categoryComponent.addExtra("\n");
            currentBuilder.append(categoryComponent);
            currentPageCategories++;
        }

        // Add custom reason option if enabled
        if (plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
            if (currentPageCategories >= (isFirstPage ? firstPageCategories : subsequentPageCategories)) {
                currentBuilder.append("\n" + ChatColor.GRAY + "Page " + currentPage + " of " + totalPages);
                pages.add(currentBuilder.create());
                
                currentBuilder = new net.md_5.bungee.api.chat.ComponentBuilder()
                    .append(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n");
                
                currentPageCategories = 0;
                isFirstPage = false;
                currentPage++;
            }

            net.md_5.bungee.api.chat.TextComponent customComponent = new net.md_5.bungee.api.chat.TextComponent();
            customComponent.addExtra(new net.md_5.bungee.api.chat.TextComponent(ChatColor.RED + "• "));
            
            net.md_5.bungee.api.chat.TextComponent customText = new net.md_5.bungee.api.chat.TextComponent("Custom Reason");
            customText.setColor(net.md_5.bungee.api.ChatColor.RED);
            customText.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                "/report " + targetPlayer + " custom"
            ));
            customText.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("Click to enter a custom reason").create()
            ));
            
            customComponent.addExtra(customText);
            customComponent.addExtra("\n");
            currentBuilder.append(customComponent);
            currentPageCategories++;
        }

        currentBuilder.append("\n" + ChatColor.GRAY + "Page " + currentPage + " of " + totalPages);
        pages.add(currentBuilder.create());

        try {
            // Create a new book meta for each page to prevent resource leaks
            if (!pages.isEmpty()) {
                // Initialize the book with empty pages first
                for (int i = 0; i < pages.size(); i++) {
                    meta.addPage(""); // Add empty pages first
                }
                book.setItemMeta(meta); // Set meta with empty pages
                
                // Now set the content for each page
                meta = (BookMeta) book.getItemMeta(); // Get fresh meta after setting empty pages
                for (int i = 0; i < pages.size(); i++) {
                    meta.spigot().setPage(i + 1, pages.get(i));
                }
                book.setItemMeta(meta);
                player.openBook(book);
            } else {
                player.sendMessage(ChatColor.RED + "No report categories available.");
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An error occurred while creating the report menu.");
            plugin.getLogger().warning("Error creating report book GUI: " + e.getMessage());
        }
    }
}