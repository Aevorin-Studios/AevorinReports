package dev.aevorinstudios.aevorinReports.sync;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.utils.ExceptionHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages token-based synchronization between Bukkit servers and proxy
 * Handles authentication, message passing, and connection state management
 */
public class TokenSyncManager implements PluginMessageListener, Listener {
    private static final String CHANNEL = "aevorinreports:sync";
    private final BukkitPlugin plugin;
    private boolean isAuthenticated = false;
    private CompletableFuture<Boolean> authFuture;
    
    // Track authentication attempts
    private int authAttempts = 0;
    private static final int MAX_AUTH_ATTEMPTS = 5;
    
    // Track message delivery status
    private final Map<UUID, CompletableFuture<Boolean>> pendingMessages = new ConcurrentHashMap<>();
    
    // Connection state
    private boolean channelRegistered = false;
    private long lastSuccessfulSync = 0;

    /**
     * Creates a new TokenSyncManager
     * 
     * @param plugin The BukkitPlugin instance
     */
    public TokenSyncManager(BukkitPlugin plugin) {
        this.plugin = plugin;
        this.authFuture = new CompletableFuture<>();
        setupMessaging();
        
        // Register for player join events to handle authentication when players join
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Schedule periodic connection health checks
        scheduleHealthCheck();
    }

    /**
     * Sets up plugin messaging channels
     */
    private void setupMessaging() {
        try {
            plugin.getLogger().info("⚜️ Registering plugin messaging channels...");
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
            Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
            channelRegistered = true;
            plugin.getLogger().info("✅ Plugin messaging channels registered successfully!");
        } catch (Exception e) {
            channelRegistered = false;
            Map<String, Object> context = new HashMap<>();
            context.put("channel", CHANNEL);
            ExceptionHandler.getInstance().handleException(e, "TokenSync Channel Setup", context);
            plugin.getLogger().severe("❌ Failed to register plugin messaging channels!");
        }
    }

    /**
     * Authenticates with the proxy server
     * 
     * @return A CompletableFuture that completes with the authentication result
     */
    public CompletableFuture<Boolean> authenticate() {
        // Reset the auth future if it's already completed
        if (authFuture.isDone()) {
            authFuture = new CompletableFuture<>();
        }
        
        // Check if proxy is enabled
        boolean proxyEnabled = plugin.getConfig().getBoolean("proxy.enabled", false);
        if (!proxyEnabled) {
            plugin.getLogger().info("✅ Proxy synchronization is disabled in config. Authentication skipped.");
            authFuture.complete(true);
            return authFuture;
        }
        
        // Validate token
        String token = plugin.getConfig().getString("auth.token");
        if (token == null || token.isEmpty() || token.equals("your_token_here")) {
            plugin.getLogger().severe("❌ Invalid token configuration! Please set a valid token in config.yml");
            authFuture.complete(false);
            return authFuture;
        }
        
        // Check if channels are registered
        if (!channelRegistered) {
            plugin.getLogger().warning("⚠️ Plugin messaging channels not registered. Attempting to register...");
            setupMessaging();
            if (!channelRegistered) {
                plugin.getLogger().severe("❌ Failed to register plugin messaging channels. Authentication aborted.");
                authFuture.complete(false);
                return authFuture;
            }
        }
        
        // Track authentication attempt
        authAttempts++;
        
        // Send authentication request to proxy if players are online
        Player player = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        if (player != null) {
            try {
                plugin.getLogger().info("🔐 Sending authentication request to proxy via " + player.getName());
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("AUTH");
                out.writeUTF(token);
                out.writeUTF(plugin.getDescription().getVersion()); // Send plugin version for compatibility check
                player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
                
                // Set timeout for authentication response
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!authFuture.isDone()) {
                        plugin.getLogger().warning("⚠️ Authentication request timed out after 10 seconds");
                        if (authAttempts < MAX_AUTH_ATTEMPTS) {
                            plugin.getLogger().info("🔄 Retrying authentication... (Attempt " + (authAttempts + 1) + "/" + MAX_AUTH_ATTEMPTS + ")");
                            authenticate();
                        } else {
                            plugin.getLogger().severe("❌ Maximum authentication attempts reached. Authentication failed.");
                            authFuture.complete(false);
                        }
                    }
                }, 200L); // 10 seconds timeout (200 ticks)
            } catch (Exception e) {
                Map<String, Object> context = new HashMap<>();
                context.put("player", player.getName());
                context.put("attempt", authAttempts);
                ExceptionHandler.getInstance().handleException(e, "TokenSync Authentication", context);
                plugin.getLogger().severe("❌ Failed to send authentication request!");
                authFuture.complete(false);
            }
        } else {
            // No players online, we'll retry when a player joins
            plugin.getLogger().info("⏳ No players online. Authentication will be attempted when a player joins.");
        }
        
        return authFuture;
    }

    /**
     * Handles incoming plugin messages
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();
            
            switch (subChannel) {
                case "AUTH_RESPONSE":
                    boolean success = in.readBoolean();
                    String serverName = "unknown";
                    String proxyVersion = "unknown";
                    
                    // Read additional information if available
                    try {
                        serverName = in.readUTF();
                        proxyVersion = in.readUTF();
                    } catch (Exception e) {
                        // Older proxy version might not send this info
                        plugin.getLogger().fine("Proxy did not send additional information");
                    }
                    
                    // Update authentication state
                    isAuthenticated = success;
                    authFuture.complete(success);
                    
                    if (success) {
                        lastSuccessfulSync = System.currentTimeMillis();
                        plugin.getLogger().info(String.format(
                            "✅ Successfully authenticated with proxy server '%s' (version: %s)!", 
                            serverName, proxyVersion));
                    } else {
                        plugin.getLogger().severe("❌ Failed to authenticate with proxy. Please check your token configuration.");
                    }
                    break;
                    
                case "MESSAGE_RESPONSE":
                    // Handle message delivery confirmation
                    String messageId = in.readUTF();
                    boolean delivered = in.readBoolean();
                    
                    CompletableFuture<Boolean> messageFuture = pendingMessages.remove(UUID.fromString(messageId));
                    if (messageFuture != null) {
                        messageFuture.complete(delivered);
                    }
                    
                    if (delivered) {
                        lastSuccessfulSync = System.currentTimeMillis();
                        plugin.getLogger().fine("Message " + messageId + " delivered successfully");
                    } else {
                        plugin.getLogger().warning("⚠️ Message " + messageId + " delivery failed");
                    }
                    break;
                    
                case "PING_RESPONSE":
                    // Handle ping response
                    lastSuccessfulSync = System.currentTimeMillis();
                    plugin.getLogger().fine("Received ping response from proxy");
                    break;
                    
                default:
                    plugin.getLogger().warning("⚠️ Received unknown subchannel: " + subChannel);
                    break;
            }
        } catch (Exception e) {
            Map<String, Object> context = new HashMap<>();
            context.put("channel", channel);
            context.put("player", player.getName());
            context.put("message_length", message.length);
            ExceptionHandler.getInstance().handleException(e, "TokenSync Message Processing", context);
            plugin.getLogger().log(Level.SEVERE, "❌ Error processing plugin message", e);
        }
    }

    /**
     * Checks if the plugin is authenticated with the proxy
     * 
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    /**
     * Sends a message to the proxy
     * 
     * @param type The message type
     * @param data The message data
     * @return A CompletableFuture that completes with the delivery result
     */
    public CompletableFuture<Boolean> sendMessage(String type, Map<String, String> data) {
        if (!isAuthenticated) {
            plugin.getLogger().warning("⚠️ Cannot send message: Not authenticated with proxy");
            return CompletableFuture.completedFuture(false);
        }
        
        Player player = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        if (player == null) {
            plugin.getLogger().warning("⚠️ Cannot send message: No players online");
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            UUID messageId = UUID.randomUUID();
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("MESSAGE");
            out.writeUTF(messageId.toString());
            out.writeUTF(type);
            out.writeInt(data.size());
            
            // Write all data entries
            for (Map.Entry<String, String> entry : data.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }
            
            // Send the message
            player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
            
            // Create and register future for response tracking
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            pendingMessages.put(messageId, future);
            
            // Set timeout for message delivery
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                CompletableFuture<Boolean> pendingFuture = pendingMessages.remove(messageId);
                if (pendingFuture != null && !pendingFuture.isDone()) {
                    pendingFuture.complete(false);
                    plugin.getLogger().warning("⚠️ Message " + messageId + " delivery timed out");
                }
            }, 100L); // 5 seconds timeout
            
            return future;
        } catch (Exception e) {
            Map<String, Object> context = new HashMap<>();
            context.put("message_type", type);
            context.put("data_size", data.size());
            ExceptionHandler.getInstance().handleException(e, "TokenSync Message Sending", context);
            plugin.getLogger().severe("❌ Failed to send message to proxy!");
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Sends a ping to the proxy to check connection
     * 
     * @return true if ping was sent, false otherwise
     */
    public boolean ping() {
        if (!isAuthenticated) {
            return false;
        }
        
        Player player = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        if (player == null) {
            return false;
        }
        
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("PING");
            out.writeLong(System.currentTimeMillis());
            player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
            return true;
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "TokenSync Ping");
            return false;
        }
    }
    
    /**
     * Schedules periodic health checks for the connection
     */
    private void scheduleHealthCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isAuthenticated && !Bukkit.getOnlinePlayers().isEmpty()) {
                // Check if we haven't received a response in a while
                long timeSinceLastSync = System.currentTimeMillis() - lastSuccessfulSync;
                if (timeSinceLastSync > TimeUnit.MINUTES.toMillis(5)) {
                    plugin.getLogger().warning("⚠️ No proxy communication for 5 minutes. Checking connection...");
                    ping();
                }
            }
        }, 6000L, 6000L); // Check every 5 minutes (6000 ticks)
    }
    
    /**
     * Handles player join events to attempt authentication if needed
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // If we're not authenticated and there's no pending authentication, try to authenticate
        if (!isAuthenticated && (authFuture.isDone() || authAttempts == 0)) {
            plugin.getLogger().info("🔄 Player joined, attempting authentication with proxy...");
            Bukkit.getScheduler().runTaskLater(plugin, this::authenticate, 20L); // Wait 1 second after join
        }
    }
    
    /**
     * Cleans up resources when the plugin is disabled
     */
    public void cleanup() {
        try {
            // Cancel all pending message futures
            for (CompletableFuture<Boolean> future : pendingMessages.values()) {
                if (!future.isDone()) {
                    future.complete(false);
                }
            }
            pendingMessages.clear();
            
            // Unregister channels
            if (channelRegistered) {
                Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
                Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
                channelRegistered = false;
            }
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "TokenSync Cleanup");
        }
    }
}