package dev.aevorinstudios.aevorinReports.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ReportReasonHolder implements InventoryHolder {
    private final String targetPlayer;
    private final int page;

    public ReportReasonHolder(String targetPlayer, int page) {
        this.targetPlayer = targetPlayer;
        this.page = page;
    }

    public String getTargetPlayer() {
        return targetPlayer;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
