package dev.aevorinstudios.aevorinReports.gui.holders;

import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ReportManageHolder implements InventoryHolder {
    private final Report report;

    public ReportManageHolder(Report report) {
        this.report = report;
    }

    public Report getReport() {
        return report;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
