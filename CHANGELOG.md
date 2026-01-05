# Changelog

All notable changes to this project will be documented in this file.

## [1.0.4.3] - 2026-01-05

### Discord Network Mode
- **Network Mode for Multi-Server Setups**: Added a new `network-mode` configuration option for Discord integration.
  - Only ONE server in your network should enable `discord.network-mode.enabled: true`
  - This designated server will poll the database for new reports from ALL servers in the network via a reliable ID-based system.
  - Configurable poll interval (default: 10 seconds).
  - Perfect for networks where you want centralized Discord notifications without running the bot on every server.
- **Improved Reliability**: Switched to ID-based tracking to ensure no reports are duplicated or missed, even across server restarts.
- **Duplication Fix**: Resolved issues where reports on the bot-server could be sent twice.

### General & Security
- **Region Restriction**: Added a restriction to prevent the plugin from running on servers located in Israel (via IP geolocation and server timezone checks) with a #FreePalestine message.
- **Improved Database Logs**: Simplified database connection error messages to be cleaner and more readable.
- **Configuration Cleanup**: Removed unused security and encryption sections for a leaner setup.

## [1.0.4.2] - 2026-01-04

### General
- **Discord Integration**: Added a robust Discord integration with a dedicated bot.
- **Improved Database Logs**: Simplified database connection error messages to be cleaner and more readable (no more giant stack traces).
- **Configuration Cleanup**: Removed the unused "Security" section and encryption key logic for a leaner `config.yml`.
- **Folia Compatibility**: Ensured all new features, including the Discord bot's background tasks, are fully compatible with Folia servers.

### Discord Integration Features
- **Dynamic Bot Presence**: Configure the bot's status (Online, Idle, etc.) and activity (Watching, Playing, etc.) with support for the `%online_players%` placeholder.
- **Real-time Notifications**: Receive beautifully formatted embeds for new reports in your Discord channel.
- **Slash Commands**:
    - `/reports`: List all active pending reports.
    - `/lookup <id>`: View detailed information about a specific report.
    - `/resolve <id>`, `/reject <id>`, `/pending <id>`: Manage report statuses directly from Discord.
    - `/help`: Detailed help menu for the bot.
- **Smart Formatting**: The "Server" field in Discord embeds automatically hides itself if you are only running a single server, matching the in-game behavior.
- **Customizable Aesthetics**: Independent color settings for report alerts and lookup results in `config.yml`.

## [1.0.4.1] - 2025-12-20

- **Changed the Book Gui's Reason to be like this: "Reason: Hover to See"**
- **Fixed a bug where the Book Gui's Status colours were always red instead of their own independent colour**

## [1.0.4] - 2025-12-19

- **Added Folia Server Software Support**
- **Made the config.yml cleaner**
- **Removed Velocity Support**
- **Added an System for Network Servers**: All servers should run the plugin (proxy excluded) and connect to the same database to share and manage reports across the entire network.

## [1.0.3] - 2025-10-23

### New Features
- **Enhanced Report Command**: Added `/report <player> <reason>` for direct reporting.
- **Custom Reasons in GUI**: Added a "Custom Reason" option to the Book GUI. Selecting this closes the GUI and prompts the player to enter a reason in chat.
- **Container GUI**: The Container GUI interface is now fully functional.
- **bStats Support**: Added bStats integration for anonymous plugin metrics.
- **Player Report History**: Recent update allows regular players to view their own submitted reports via `/reports`.
- **MiniMessage Support**: Added support for MiniMessage formatting (e.g., `<red>`, `<gradient>`) in configuration and messages.
- **Update Checker**: Implemented an update checker using the Modrinth API.

### Changes & Improvements
- **Command Renaming**: Renamed `/shiftreport <id> <status>` to `/setreportstatus <id> to <status>` for better clarity.
- **Smart Permissions**: The `/viewreport` command now allows players to view their own reports without needing admin permissions.
- **GUI Experience**: 
  - Improved Book GUI readability (Fixed invisible Report IDs).
  - Status change actions now close the inventory for a smoother workflow.
  - "Your Reports" view added for non-staff players.
- **Console Output**: Cleaned up console logs by removing emojis that were causing display issues on some terminals.
- **Notifications**: Updated staff notifications and success messages to use modern MiniMessage formatting and allow full customization in `config.yml`.
- **Codebase Optimization**: Comprehensive cleanup of imports, command registration, and error handling logic.

### Removed
- **Legacy Command**: Removed `/report-category <player> <reason>` as it has been replaced by the streamlined `/report` command.
