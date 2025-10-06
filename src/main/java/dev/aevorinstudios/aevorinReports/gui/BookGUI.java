package dev.aevorinstudios.aevorinReports.gui;

import org.bukkit.entity.Player;
import dev.aevorinstudios.aevorinReports.reports.Report;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import dev.aevorinstudios.aevorinReports.config.ReportConfiguration;
import dev.aevorinstudios.aevorinReports.config.GUIConfiguration;
import dev.aevorinstudios.aevorinReports.config.MessagesConfiguration;
import dev.aevorinstudios.aevorinReports.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class BookGUI {
    private final List<Component> pages;
    private final Player viewer;
    private int currentPage;
    private final ReportConfiguration reportConfig;
    private final GUIConfiguration guiConfig;
    private final MessagesConfiguration messagesConfig;

    public BookGUI(Player viewer) {
        this.viewer = viewer;
        this.pages = new ArrayList<>();
        this.currentPage = 0;
        this.reportConfig = ReportConfiguration.getInstance();
        this.guiConfig = GUIConfiguration.getInstance();
        this.messagesConfig = MessagesConfiguration.getInstance();
        initializePages();
    }

    private void initializePages() {
        Component firstPage = Component.empty()
                .append(Component.text(messagesConfig.getMainTitle(), ColorUtils.parseColor(guiConfig.getTitleColor()))
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.newline())
                .append(Component.text(guiConfig.getSeparatorLine(), ColorUtils.parseColor(guiConfig.getSeparatorColor())))
                .append(Component.newline());

        List<String> categories = reportConfig.getCategories();
        for (String category : categories) {
            firstPage = firstPage
                    .append(createReportOption(category))
                    .append(Component.newline());
        }

        firstPage = firstPage.append(Component.text(guiConfig.getSeparatorLine(), ColorUtils.parseColor(guiConfig.getSeparatorColor())));
        pages.add(firstPage);
    }

    private Component createReportOption(String reason) {
        return Component.text(guiConfig.getGroupBullet(), ColorUtils.parseColor(guiConfig.getSeparatorColor()))
                .append(Component.text(reason, ColorUtils.parseColor(guiConfig.getPlayerNameColor()))
                        .clickEvent(ClickEvent.runCommand("/report " + reason))
                        .hoverEvent(HoverEvent.showText(Component.text(messagesConfig.getViewDetails(), ColorUtils.parseColor(guiConfig.getNumberColor())))));
    }
    public void addReportPage(List<Report> reports) {
        Component page = Component.empty()
                .append(Component.text(messagesConfig.getGroupTitle(), ColorUtils.parseColor(guiConfig.getTitleColor()))
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.newline())
                .append(Component.text(guiConfig.getSeparatorLine(), ColorUtils.parseColor(guiConfig.getSeparatorColor())))
                .append(Component.newline());

        int count = 0;
        for (Report report : reports) {
            if (count >= guiConfig.getReportsPerPage()) {
                pages.add(page);
                page = Component.empty();
                count = 0;
            }

            String statusColor = switch (report.getStatus()) {
                case PENDING -> guiConfig.getPendingColor();
                case RESOLVED -> guiConfig.getResolvedColor();
                case REJECTED -> guiConfig.getRejectedColor();
            };

            page = page.append(Component.text(messagesConfig.getNumberPrefix() + report.getId(), ColorUtils.parseColor(guiConfig.getNumberColor())))
                    .append(Component.text(guiConfig.getAfterNumber()))
                    .append(Component.text(messagesConfig.formatStatusFormat(report.getStatus().name()), ColorUtils.parseColor(statusColor)))
                    .append(Component.text(guiConfig.getAfterName()))
                    .append(Component.text(report.getCategory(), ColorUtils.parseColor(guiConfig.getPlayerNameColor())))
                    .append(Component.newline());

            count++;
        }

        if (count > 0) {
            page = page.append(Component.text(guiConfig.getSeparatorLine(), ColorUtils.parseColor(guiConfig.getSeparatorColor())));
            pages.add(page);
        }
    }

    public void show() {
        if (currentPage >= 0 && currentPage < pages.size()) {
            viewer.sendMessage(pages.get(currentPage));
        }
    }

    public void nextPage() {
        if (currentPage < pages.size() - 1) {
            currentPage++;
            show();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            show();
        }
    }
}