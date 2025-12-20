# Changelog

All notable changes to this project will be documented in this file.

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
