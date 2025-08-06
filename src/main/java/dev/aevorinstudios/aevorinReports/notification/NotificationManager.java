package dev.aevorinstudios.aevorinReports.notification;

import dev.aevorinstudios.aevorinReports.config.Settings;
import dev.aevorinstudios.aevorinReports.model.Report;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public class NotificationManager {
    private final Settings settings;
    private final MiniMessage miniMessage;

    public NotificationManager(Settings settings) {
        this.settings = settings;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void sendNewReportNotification(Report report) {
        if (!settings.getNotifications().isNewReport()) {
            return;
        }

        String message = settings.getMessages().getReportNotification()
                .replace("{reporter}", report.getReporter().toString())
                .replace("{reported}", report.getReported().toString())
                .replace("{category}", report.getCategory());

        sendToStaff(message, true);
    }

    public void sendStatusChangeNotification(Report report) {
        if (!settings.getNotifications().isStatusChange()) {
            return;
        }

        String message = settings.getMessages().getReportStatusChange()
                .replace("{id}", String.valueOf(report.getId()))
                .replace("{status}", report.getStatus().toString());

        sendToStaff(message, true);
    }

    private void sendToStaff(String message, boolean playSound) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        Component component = miniMessage.deserialize(
                settings.getNotifications().getPrefix() + message
        );

        for (Player player : players) {
            if (player.hasPermission("aevorinreports.notifications")) {
                ((Audience) player).sendMessage(component);
                if (playSound && settings.getNotifications().getSound() != null) {
                    player.playSound(
                            player.getLocation(),
                            settings.getNotifications().getSound(),
                            1.0f,
                            1.0f
                    );
                }
            }
        }
    }

    public void sendMessage(Audience audience, String message) {
        Component component = miniMessage.deserialize(
                settings.getNotifications().getPrefix() + message
        );
        audience.sendMessage(component);
    }

    public void sendMessage(Audience audience, Component component) {
        Component prefixedComponent = miniMessage.deserialize(settings.getNotifications().getPrefix())
                .append(component);
        audience.sendMessage(prefixedComponent);
    }
}