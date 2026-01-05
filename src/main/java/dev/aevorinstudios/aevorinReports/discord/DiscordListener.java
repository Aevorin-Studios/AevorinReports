package dev.aevorinstudios.aevorinReports.discord;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DiscordListener extends ListenerAdapter {
    private final BukkitPlugin plugin;

    public DiscordListener(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("Commands can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        if (!hasPermission(event.getMember())) {
            event.reply("You don't have permission to manage reports.").setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "resolve" -> handleSetStatusSlash(event, Report.ReportStatus.RESOLVED);
            case "reject" -> handleSetStatusSlash(event, Report.ReportStatus.REJECTED);
            case "pending" -> handleSetStatusSlash(event, Report.ReportStatus.PENDING);
            case "lookup" -> handleLookupSlash(event);
            case "reports" -> handleListReportsSlash(event);
            case "help" -> handleHelpSlash(event);
        }
    }

    private void handleHelpSlash(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("AevorinReports Help")
                .setDescription("Manage Minecraft reports from Discord using official Slash Commands.")
                .addField("/reports", "List all active pending reports.", false)
                .addField("/lookup <id>", "Show detailed info about a report.", false)
                .addField("/resolve <id>", "Mark a report as resolved.", false)
                .addField("/reject <id>", "Mark a report as rejected.", false)
                .addField("/pending <id>", "Move a report back to pending.", false)
                .addField("/help", "Show this help menu.", false)
                .setColor(Color.WHITE);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleSetStatusSlash(SlashCommandInteractionEvent event, Report.ReportStatus status) {
        long id = event.getOption("id").getAsLong();
        Report report = plugin.getDatabaseManager().getReport(id);

        if (report == null) {
            event.reply("Report #" + id + " not found.").setEphemeral(true).queue();
            return;
        }

        if (report.getStatus() == status) {
            event.reply("Report #" + id + " is already " + status.name().toLowerCase() + ".").setEphemeral(true).queue();
            return;
        }

        report.setStatus(status);
        report.setLastUpdatedBy("Discord:" + event.getUser().getName());
        plugin.getDatabaseManager().updateReport(report);

        // Professional Embed for the Ephemeral success message
        EmbedBuilder successEmbed = new EmbedBuilder()
                .setTitle("Report Updated")
                .setDescription("Successfully updated Report **#" + id + "** to **" + status.name().toLowerCase() + "**.")
                .setColor(status == Report.ReportStatus.RESOLVED ? Color.GREEN : (status == Report.ReportStatus.REJECTED ? Color.RED : Color.ORANGE));

        event.replyEmbeds(successEmbed.build()).setEphemeral(true).queue();

        // Public log message to log channel
        plugin.getDiscordManager().sendLogUpdate(report, event.getUser().getAsMention());
    }

    private void handleLookupSlash(SlashCommandInteractionEvent event) {
        long id = event.getOption("id").getAsLong();
        Report report = plugin.getDatabaseManager().getReport(id);

        if (report == null) {
            event.reply("Report #" + id + " not found.").setEphemeral(true).queue();
            return;
        }

        String reporter = PlayerNameResolver.resolvePlayerName(report.getReporterUuid());
        String reported = PlayerNameResolver.resolvePlayerName(report.getReportedUuid());

        String colorHex = plugin.getConfig().getString("discord.lookup-color", "#00ffff");
        Color color = Color.CYAN;
        try {
            color = Color.decode(colorHex);
        } catch (NumberFormatException ignored) {}

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Report Details: #" + id)
                .addField("Reporter", reporter, true)
                .addField("Reported Player", reported, true)
                .addField("Reason", report.getReason(), false);

        if (plugin.getDatabaseManager().hasMultipleServers()) {
            embed.addField("Server", report.getServerName(), true);
        }

        embed.addField("Status", report.getStatus().name(), true)
                .addField("Location", (report.getWorld() != null ? report.getWorld() : "Unknown") + " (" + (report.getCoordinates() != null ? report.getCoordinates() : "Unknown") + ")", false)
                .addField("Submitted At", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false)
                .setColor(color);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleListReportsSlash(SlashCommandInteractionEvent event) {
        List<Report> activeReports = plugin.getDatabaseManager().getActiveReports();

        if (activeReports.isEmpty()) {
            event.reply("There are no active reports.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Active Reports")
                .setColor(Color.ORANGE);

        StringBuilder sb = new StringBuilder();
        for (Report report : activeReports) {
            String reported = PlayerNameResolver.resolvePlayerName(report.getReportedUuid());
            sb.append("`#").append(report.getId()).append("` - **").append(reported).append("** (").append(report.getReason()).append(")\n");
            
            if (sb.length() > 1800) {
                sb.append("*...and more*");
                break;
            }
        }

        embed.setDescription(sb.toString());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private boolean hasPermission(Member member) {
        if (member == null) return false;
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER)) return true;

        String roleId = plugin.getConfig().getString("discord.staff-role-id");
        if (roleId != null && !roleId.isEmpty()) {
            for (Role role : member.getRoles()) {
                if (role.getId().equals(roleId)) return true;
            }
        }

        return false;
    }
}
