package dev.aevorinstudios.aevorinReports.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PlayerReportsHolder implements InventoryHolder {
    private final int page;

    public PlayerReportsHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used
    }
}
