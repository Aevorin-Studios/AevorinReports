package dev.aevorinstudios.aevorinReports.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.aevorinstudios.aevorinReports.gui.VelocityBookGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DebugReportCommand implements SimpleCommand {

    @Override
    public void execute(final Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            player.sendMessage(Component.text("Debug Report Commands:", NamedTextColor.GOLD)
                    .append(Component.text("\n/debugreport gui", NamedTextColor.YELLOW)
                            .append(Component.text(" - Opens the report GUI in debug mode", NamedTextColor.GRAY)))
                    .append(Component.text("\n/debugreport submit <reason>", NamedTextColor.YELLOW)
                            .append(Component.text(" - Simulates submitting a report", NamedTextColor.GRAY))));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> {
                VelocityBookGUI gui = new VelocityBookGUI(player);
                gui.show();
                player.sendMessage(Component.text("Opened report GUI in debug mode", NamedTextColor.GREEN));
            }
            case "submit" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /debugreport submit <reason>", NamedTextColor.RED));
                    return;
                }
                String reason = String.join(" ", args).substring(args[0].length() + 1);
                // TODO: Implement debug report submission logic
                player.sendMessage(Component.text("Debug report submitted with reason: " + reason, NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("Unknown debug command. Use /debugreport for help", NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("aevorinreports.debug");
    }
}