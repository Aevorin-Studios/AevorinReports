package dev.aevorinstudios.aevorinReports.bukkit.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.aevorinstudios.aevorinReports.config.ConfigManager.getInstance;

public class BukkitReportsCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();
    private final Map<Report.ReportStatus, List<Report>> reportCache = new HashMap<>();
    private static final long COMMAND_COOLDOWN = 500; // 500 ms cooldown
    private static final long CACHE_DURATION = 5000; // 5 seconds cache duration
    private long lastCacheUpdate = 0;

    public BukkitReportsCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("aevorinreports.manage")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> groups = new ArrayList<>();
            String partialGroup = args[0].toUpperCase();
            
            for (Report.ReportStatus status : Report.ReportStatus.values()) {
                if (status.name().startsWith(partialGroup)) {
                    groups.add(status.name());
                }
            }
            
            return groups;
        }
        
        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getInstance().getMessage("messages.errors.player-only"));
            return true;
        }

        if (!player.hasPermission("aevorinreports.manage")) {
            sender.sendMessage(getInstance().getMessage("messages.errors.no-permission"));
            return true;
        }

        if (args.length == 0) {
            showReportsBook(player);
            return true;
        }

        String group = args[0].toUpperCase();
        Report.ReportStatus status;
        try {
            status = Report.ReportStatus.valueOf(group);
        } catch (IllegalArgumentException e) {
            player.sendMessage(getInstance().getMessage("messages.errors.invalid-group"));
            return true;
        }

        showReportsBookByStatus(player, status);
        return true;
    }

    private void showReportsBook(Player player) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");
        if (guiType.equalsIgnoreCase("container")) {
            showReportsContainerGUI(player);
            return;
        }

        // Check command cooldown
        long currentTime = System.currentTimeMillis();
        long lastTime = lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastTime < COMMAND_COOLDOWN) {
            return;
        }
        lastCommandTime.put(player.getUniqueId(), currentTime);

        // Update cache if needed
        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            reportCache.clear();
            lastCacheUpdate = currentTime;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) {
            String errorMsg = getInstance().getMessage("messages.errors.book-meta-null");
            player.sendMessage(errorMsg != null ? errorMsg : "Error creating book.");
            return;
        }

        String title = getInstance().getMessage("messages.book.main-title");
        meta.setTitle(title != null ? title : "Report Management");
        meta.setAuthor("Server Admin");

        List<net.kyori.adventure.text.Component> pages = new ArrayList<>();
        net.kyori.adventure.text.Component mainPage = net.kyori.adventure.text.Component.empty()
            .append(net.kyori.adventure.text.Component.text("Report Categories", 
                net.kyori.adventure.text.format.NamedTextColor.DARK_RED))
            .append(net.kyori.adventure.text.Component.newline())
            .append(net.kyori.adventure.text.Component.newline());

        for (Report.ReportStatus status : Report.ReportStatus.values()) {
            List<Report> reports = reportCache.computeIfAbsent(status, k -> DatabaseManager.getInstance().getReportsByStatus(status));
            String groupName = status.name();
            int count = reports.size();

            // Get the bullet symbol with null check
            String bulletSymbol = getInstance().getSymbol("group_bullet");
            bulletSymbol = (bulletSymbol != null) ? bulletSymbol : "• ";
            
            // Format the hover text
            String hoverText = getInstance().getFormattedMessage("messages.book.view-group", "{count}", String.valueOf(count), "{status}", groupName.toLowerCase());
            if (hoverText == null || hoverText.isEmpty()) {
                hoverText = "View " + count + " " + groupName.toLowerCase() + " reports";
            }

            net.kyori.adventure.text.Component groupComponent = net.kyori.adventure.text.Component.text(
                bulletSymbol + groupName + " ",
                net.kyori.adventure.text.format.NamedTextColor.GOLD
            )
            .append(net.kyori.adventure.text.Component.text(
                "(" + count + ")",
                net.kyori.adventure.text.format.NamedTextColor.GREEN
            ))
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/reports " + groupName))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                net.kyori.adventure.text.Component.text(hoverText)
            ));

            mainPage = mainPage.append(groupComponent)
                      .append(net.kyori.adventure.text.Component.newline())
                      .append(net.kyori.adventure.text.Component.newline());
        }

        pages.add(mainPage);
        meta.pages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    private void showReportsContainerGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "Reports Menu");

        // Pending Reports
        ItemStack pending = new ItemStack(Material.PAPER);
        org.bukkit.inventory.meta.ItemMeta pendingMeta = pending.getItemMeta();
        pendingMeta.setDisplayName("§ePending Reports");
        pendingMeta.setLore(java.util.List.of(
            "§7View all pending reports.",
            "§eClick to view!"
        ));
        pending.setItemMeta(pendingMeta);
        gui.setItem(10, pending);

        // Resolved Reports
        ItemStack resolved = new ItemStack(Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta resolvedMeta = resolved.getItemMeta();
        resolvedMeta.setDisplayName("§aResolved Reports");
        resolvedMeta.setLore(java.util.List.of(
            "§7View all resolved reports.",
            "§eClick to view!"
        ));
        resolved.setItemMeta(resolvedMeta);
        gui.setItem(13, resolved);

        // Rejected Reports
        ItemStack rejected = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta rejectedMeta = rejected.getItemMeta();
        rejectedMeta.setDisplayName("§cRejected Reports");
        rejectedMeta.setLore(java.util.List.of(
            "§7View all rejected reports.",
            "§eClick to view!"
        ));
        rejected.setItemMeta(rejectedMeta);
        gui.setItem(16, rejected);

        player.openInventory(gui);
    }

    private void showReportsBookByStatus(Player player, Report.ReportStatus status) {
        // Check command cooldown
        long currentTime = System.currentTimeMillis();
        long lastTime = lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastTime < COMMAND_COOLDOWN) {
            return;
        }
        lastCommandTime.put(player.getUniqueId(), currentTime);

        // Update cache if needed
        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            reportCache.clear();
            lastCacheUpdate = currentTime;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) {
            String errorMsg = getInstance().getMessage("messages.errors.book-meta-null");
            player.sendMessage(errorMsg != null ? errorMsg : "Error creating book.");
            return;
        }

        String groupName = status.name();
        String title = getInstance().getFormattedMessage("messages.book.group-title", "{status}", status.name(), "{group}", groupName.toLowerCase());
        meta.setTitle(title != null ? title : status.name() + " Reports");
        meta.setAuthor("Server Admin");

        List<net.kyori.adventure.text.Component> pages = new ArrayList<>();
        List<Report> reports = reportCache.computeIfAbsent(status, k -> DatabaseManager.getInstance().getReportsByStatus(status));
        // Ensure reports are not null
        if (reports == null) {
            String errorMsg = getInstance().getMessage("messages.errors.no-reports");
            player.sendMessage(errorMsg != null ? errorMsg : "No reports found.");
            return;
        }

        if (reports.isEmpty()) {
            String noReportsMessage = getInstance().getFormattedMessage("messages.errors.no-reports", "{status}", status.name().toLowerCase(), "", "");
            if (noReportsMessage == null || noReportsMessage.isEmpty()) {
                noReportsMessage = "No " + status.name().toLowerCase() + " reports found.";
            }
            
            net.kyori.adventure.text.Component emptyPage = net.kyori.adventure.text.Component.text(
                noReportsMessage,
                net.kyori.adventure.text.format.NamedTextColor.RED
            )
            .append(net.kyori.adventure.text.Component.newline())
            .append(net.kyori.adventure.text.Component.newline())
            .append(net.kyori.adventure.text.Component.text(
                "[Back to Categories]",
                net.kyori.adventure.text.format.NamedTextColor.BLUE
            )
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/reports"))
            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                net.kyori.adventure.text.Component.text("Return to report categories", net.kyori.adventure.text.format.NamedTextColor.BLUE).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
            )));
            
            pages.add(emptyPage);
        } else {
            int reportsPerPage = getInstance().getReportsPerPage();
            if (reportsPerPage <= 0) reportsPerPage = 5; // Fallback if config returns invalid value
            
            // Add header to first page
            net.kyori.adventure.text.Component currentPage = net.kyori.adventure.text.Component.empty()
                .append(net.kyori.adventure.text.Component.text(
                    status.name() + " Reports",
                    net.kyori.adventure.text.format.NamedTextColor.DARK_RED
                ))
                .append(net.kyori.adventure.text.Component.newline())
                .append(net.kyori.adventure.text.Component.text(
                    "Total: " + reports.size(),
                    net.kyori.adventure.text.format.NamedTextColor.GRAY
                ))
                .append(net.kyori.adventure.text.Component.newline())
                .append(net.kyori.adventure.text.Component.newline());

            int count = 0;
            int reportNumber = 1;
            
            for (Report report : reports) {
                if (count > 0 && count % reportsPerPage == 0) {
                    // Add back button at the bottom of each page
                    currentPage = currentPage.append(net.kyori.adventure.text.Component.newline())
                        .append(net.kyori.adventure.text.Component.text(
                            "[Back to Categories]",
                            net.kyori.adventure.text.format.NamedTextColor.BLUE
                        )
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/reports"))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            net.kyori.adventure.text.Component.text("Return to report categories")
                        )));
                    
                    pages.add(currentPage);
                    currentPage = net.kyori.adventure.text.Component.empty();
                }

                String reportedName = PlayerNameResolver.resolvePlayerName(report.getReported());
                reportedName = (reportedName != null) ? reportedName : "Unknown Player";

                // Format the number prefix
                String numberPrefix = "#";
                String afterNumberSpacing = ": ";
                
                // Try to get values from config
                Object numberPrefixObj = getInstance().getText("messages.report-format.number-prefix");
                Object afterNumberSpacingObj = getInstance().getText("gui.spacing.after_number");
                
                // Use config values if available
                if (numberPrefixObj != null && !numberPrefixObj.toString().isEmpty()) {
                    numberPrefix = numberPrefixObj.toString();
                }
                
                if (afterNumberSpacingObj != null && !afterNumberSpacingObj.toString().isEmpty()) {
                    afterNumberSpacing = afterNumberSpacingObj.toString();
                }
                
                net.kyori.adventure.text.Component numberComponent = net.kyori.adventure.text.Component.text(
                    numberPrefix + reportNumber + afterNumberSpacing, 
                    net.kyori.adventure.text.format.NamedTextColor.GOLD
                );

                // Get hover text for player name
                String hoverText = "Click to view details for report #" + report.getId();
                Object hoverTextObj = getInstance().getMessage("messages.book.view-details");
                if (hoverTextObj != null && !hoverTextObj.toString().isEmpty()) {
                    hoverText = hoverTextObj.toString();
                }
                
                net.kyori.adventure.text.Component nameComponent = net.kyori.adventure.text.Component.text(
                    reportedName,
                    net.kyori.adventure.text.format.NamedTextColor.RED
                )
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/viewreport " + report.getId()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    net.kyori.adventure.text.Component.text(hoverText)
                ));

                currentPage = currentPage.append(numberComponent)
                          .append(nameComponent)
                          .append(net.kyori.adventure.text.Component.newline())
                          .append(net.kyori.adventure.text.Component.newline());

                count++;
                reportNumber++;
            }

            if (count > 0) {
                // Add back button to the last page
                currentPage = currentPage.append(net.kyori.adventure.text.Component.newline())
                    .append(net.kyori.adventure.text.Component.text(
                        "[Back to Categories]",
                        net.kyori.adventure.text.format.NamedTextColor.BLUE
                    )
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/reports"))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                        net.kyori.adventure.text.Component.text("Return to report categories")
                    )));
                
                pages.add(currentPage);
            }
        }

        meta.pages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }
}