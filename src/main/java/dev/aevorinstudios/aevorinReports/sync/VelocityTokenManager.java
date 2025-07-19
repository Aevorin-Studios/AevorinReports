package dev.aevorinstudios.aevorinReports.sync;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.aevorinstudios.aevorinReports.velocity.VelocityPlugin;

import java.util.HashMap;
import java.util.Map;

public class VelocityTokenManager {
    private static final String CHANNEL_ID = "aevorinreports:sync";
    private final MinecraftChannelIdentifier channel;
    private final VelocityPlugin plugin;
    private final ProxyServer server;
    private final String token;

    public VelocityTokenManager(VelocityPlugin plugin, ProxyServer server, String token) {
        this.plugin = plugin;
        this.server = server;
        this.token = token;
        this.channel = MinecraftChannelIdentifier.from(CHANNEL_ID);
        registerChannel();
    }

    private void registerChannel() {
        server.getChannelRegistrar().register(channel);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) {
            return;
        }

        // Ensure the sender is a player
        if (!(event.getSource() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getSource();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();

        switch (subChannel) {
            case "AUTH":
                handleAuthMessage(player, in);
                break;
            case "MESSAGE":
                handleReportMessage(player, in);
                break;
            case "PING":
                handlePingMessage(player, in);
                break;
            default:
                plugin.getLogger().warn("Received unknown subchannel: " + subChannel + " from server " 
                    + player.getCurrentServer().get().getServerInfo().getName());
                break;
        }
    }
    
    private void handleAuthMessage(Player player, ByteArrayDataInput in) {
        try {
            String receivedToken = in.readUTF();
            String pluginVersion = "unknown";
            
            // Try to read plugin version if available
            try {
                pluginVersion = in.readUTF();
            } catch (Exception e) {
                // Older plugin version might not send this info
            }
            
            boolean isValid = token.equals(receivedToken);

            // Send authentication response
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("AUTH_RESPONSE");
            out.writeBoolean(isValid);
            out.writeUTF(player.getCurrentServer().get().getServerInfo().getName());
            out.writeUTF(plugin.getClass().getPackage().getImplementationVersion() != null ? 
                plugin.getClass().getPackage().getImplementationVersion() : "unknown");

            player.sendPluginMessage(channel, out.toByteArray());

            if (isValid) {
                plugin.getLogger().info("Server " + player.getCurrentServer().get().getServerInfo().getName() 
                    + " (version: " + pluginVersion + ") authenticated successfully");
            } else {
                plugin.getLogger().warn("Failed authentication attempt from server " 
                    + player.getCurrentServer().get().getServerInfo().getName());
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error processing AUTH message", e);
        }
    }
    
    private void handleReportMessage(Player player, ByteArrayDataInput in) {
        try {
            String messageId = in.readUTF();
            String messageType = in.readUTF();
            int dataSize = in.readInt();
            
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < dataSize; i++) {
                String key = in.readUTF();
                String value = in.readUTF();
                data.put(key, value);
            }
            
            // Process the report message based on type
            boolean success = processReportMessage(messageType, data, player);
            
            // Send delivery confirmation
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("MESSAGE_RESPONSE");
            out.writeUTF(messageId);
            out.writeBoolean(success);
            
            player.sendPluginMessage(channel, out.toByteArray());
            
            plugin.getLogger().info("Processed " + messageType + " message from " 
                + player.getCurrentServer().get().getServerInfo().getName() 
                + (success ? " successfully" : " with errors"));
        } catch (Exception e) {
            plugin.getLogger().error("Error processing MESSAGE", e);
        }
    }
    
    private boolean processReportMessage(String messageType, Map<String, String> data, Player player) {
        try {
            // Log the received message for debugging
            plugin.getLogger().info("Received message type: " + messageType);
            
            if ("NEW_REPORT".equals(messageType)) {
                return handleNewReport(data, player);
            } else {
                plugin.getLogger().warn("Unknown message type: " + messageType);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error in processReportMessage", e);
            return false;
        }
    }
    
    private boolean handleNewReport(Map<String, String> data, Player player) {
        try {
            // Log the report data
            plugin.getLogger().info("Processing new report from server: " + 
                player.getCurrentServer().get().getServerInfo().getName());
            
            // Extract report data
            long reportId = Long.parseLong(data.get("id"));
            String reporterUuid = data.get("reporter_uuid");
            String reporterName = data.get("reporter_name");
            String reportedUuid = data.get("reported_uuid");
            String reportedName = data.get("reported_name");
            String reason = data.get("reason");
            String serverName = data.get("server_name");
            String status = data.get("status");
            
            // TODO: Store the report in the proxy's database or forward to other servers
            // For now, just broadcast to staff members on the proxy
            server.getAllPlayers().forEach(staffPlayer -> {
                if (staffPlayer.hasPermission("aevorinreports.notify")) {
                    staffPlayer.sendMessage(net.kyori.adventure.text.Component.text()
                        .content("[Reports] ")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                        .append(net.kyori.adventure.text.Component.text(reporterName)
                            .color(net.kyori.adventure.text.format.NamedTextColor.RED))
                        .append(net.kyori.adventure.text.Component.text(" has reported ")
                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .append(net.kyori.adventure.text.Component.text(reportedName)
                            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                        .append(net.kyori.adventure.text.Component.text(" for: ")
                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .append(net.kyori.adventure.text.Component.text(reason)
                            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                        .append(net.kyori.adventure.text.Component.text(" on server: ")
                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .append(net.kyori.adventure.text.Component.text(serverName)
                            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA))
                        .build());
                }
            });
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().error("Error processing new report", e);
            return false;
        }
    }
    
    private void handlePingMessage(Player player, ByteArrayDataInput in) {
        try {
            long timestamp = 0;
            try {
                timestamp = in.readLong();
            } catch (Exception e) {
                // Ignore if timestamp is not available
            }
            
            // Send ping response
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PING_RESPONSE");
            out.writeLong(System.currentTimeMillis());
            
            player.sendPluginMessage(channel, out.toByteArray());
            
            plugin.getLogger().info("Ping received from " + player.getCurrentServer().get().getServerInfo().getName());
        } catch (Exception e) {
            plugin.getLogger().error("Error processing PING message", e);
        }
    }

    public void cleanup() {
        server.getChannelRegistrar().unregister(channel);
    }
}