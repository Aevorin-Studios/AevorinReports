package dev.aevorinstudios.aevorinReports.listeners;

import dev.aevorinstudios.aevorinReports.commands.HiddenCommandManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.TabCompleteEvent;

public class CommandHidingListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        event.getCommands().removeIf(HiddenCommandManager::isHiddenCommand);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (buffer.startsWith("/")) {
            String command = buffer.substring(1).split(" ")[0];
            if (HiddenCommandManager.isHiddenCommand(command)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().split(" ")[0];
        if (HiddenCommandManager.isHiddenCommand(command)) {
            event.setCancelled(true);
            // Still execute the command but hide it from console
            event.getSender().getServer().dispatchCommand(event.getSender(), event.getCommand());
        }
    }
}
