package dev.aevorinstudios.aevorinReports.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.aevorinstudios.aevorinReports.gui.VelocityBookGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReportCommand implements SimpleCommand {

    @Override
    public void execute(final Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            // Show the report GUI when no arguments are provided
            VelocityBookGUI gui = new VelocityBookGUI(player);
            gui.show();
            return;
        }

        // Handle report submission logic here when arguments are provided
        // This will be called when a player clicks a report option
        String reason = String.join(" ", args);
        player.sendMessage(Component.text("Your report has been submitted with reason: " + reason, NamedTextColor.GREEN));
        // TODO: Add database integration for storing reports
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("aevorinreports.report");
    }
}