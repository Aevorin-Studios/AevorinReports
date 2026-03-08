package dev.aevorinstudios.aevorinReports.gui.holders;

import dev.aevorinstudios.aevorinReports.reports.Report;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class CategoryReportsHolder implements InventoryHolder {
    private final Report.ReportStatus status;
    private final int page;

    public CategoryReportsHolder(Report.ReportStatus status, int page) {
        this.status = status;
        this.page = page;
    }

    public Report.ReportStatus getStatus() {
        return status;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
