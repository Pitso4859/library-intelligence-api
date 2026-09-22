package com.pitso.ui.view;

import com.pitso.ui.api.LibraryApiClient;
import com.pitso.ui.model.Book;
import com.pitso.ui.model.CatalogInsights;
import com.pitso.ui.model.InventoryStats;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class DashboardPanel extends JPanel {
    private final LibraryApiClient api;
    private final JLabel totalValue = statValue("—");
    private final JLabel ebookValue = statValue("—");
    private final JLabel printValue = statValue("—");
    private final JLabel authorsValue = statValue("—");
    private final DefaultTableModel recentModel = new DefaultTableModel(
            new Object[]{"Type", "Title", "Author", "ISBN", "Added"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable recentTable = new JTable(recentModel);
    private final JLabel status = Ui.muted("Ready");

    public DashboardPanel(LibraryApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        build();
    }

    private void build() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Ui.heading("Dashboard", 28));
        text.add(Box.createVerticalStrut(4));
        text.add(Ui.muted("Overview of your Library Intelligence catalog."));
        top.add(text, BorderLayout.WEST);
        JButton refresh = Ui.secondaryButton("Refresh");
        refresh.addActionListener(e -> refresh());
        top.add(refresh, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(statCard("Total books", totalValue, "All catalog items"));
        stats.add(statCard("EBooks", ebookValue, "Digital titles"));
        stats.add(statCard("Print books", printValue, "Physical titles"));
        stats.add(statCard("Authors", authorsValue, "Unique authors"));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        content.add(stats);
        content.add(Box.createVerticalStrut(18));

        JPanel tableCard = Ui.panel(new BorderLayout(0, 12));
        tableCard.setBorder(Ui.cardBorder());
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(Ui.heading("Newest books", 18), BorderLayout.WEST);
        titleRow.add(status, BorderLayout.EAST);
        tableCard.add(titleRow, BorderLayout.NORTH);
        Ui.styleTable(recentTable);
        recentTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(recentTable);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.setPreferredSize(new Dimension(800, 360));
        content.add(tableCard);
        add(content, BorderLayout.CENTER);
    }

    private JPanel statCard(String title, JLabel value, String subtitle) {
        JPanel card = Ui.panel(new BorderLayout(0, 8));
        card.setBorder(Ui.cardBorder());
        card.add(Ui.muted(title), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(Ui.muted(subtitle), BorderLayout.SOUTH);
        return card;
    }

    private static JLabel statValue(String value) {
        JLabel label = new JLabel(value);
        label.setForeground(Ui.TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 30));
        return label;
    }

    public void refresh() {
        status.setText("Loading...");
        runAsync(() -> new DashboardData(api.getStats(), api.getInsights()), data -> {
            InventoryStats stats = data.stats();
            CatalogInsights insights = data.insights();
            totalValue.setText(String.valueOf(stats.totalBooks()));
            ebookValue.setText(String.valueOf(stats.totalEBooks()));
            printValue.setText(String.valueOf(stats.totalPrintBooks()));
            authorsValue.setText(String.valueOf(insights.uniqueAuthors()));
            recentModel.setRowCount(0);
            List<Book> newest = insights.newestBooks();
            for (Book b : newest) {
                recentModel.addRow(new Object[]{b.bookType(), b.title(), b.author(), b.isbnNo(), compactDate(b.createdAt())});
            }
            status.setText("Updated");
        });
    }

    private String compactDate(String value) {
        if (value == null) return "";
        return value.length() >= 10 ? value.substring(0, 10) : value;
    }

    private <T> void runAsync(Callable<T> task, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return task.call(); }
            protected void done() {
                try { success.accept(get()); }
                catch (Exception ex) {
                    status.setText("Could not load dashboard");
                    JOptionPane.showMessageDialog(DashboardPanel.this, rootMessage(ex), "API error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    private record DashboardData(InventoryStats stats, CatalogInsights insights) {}
}
