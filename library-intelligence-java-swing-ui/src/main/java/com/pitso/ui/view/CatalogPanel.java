package com.pitso.ui.view;

import com.pitso.ui.api.LibraryApiClient;
import com.pitso.ui.model.CatalogInsights;
import com.pitso.ui.model.Recommendation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class CatalogPanel extends JPanel {
    private final LibraryApiClient api;
    private final JTextField query = Ui.textField(22);
    private final JComboBox<String> type = new JComboBox<>(new String[]{"ANY", "EBOOK", "PRINTBOOK"});
    private final JSpinner limit = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
    private final DefaultTableModel recommendationsModel = new DefaultTableModel(
            new Object[]{"Score", "Type", "Title", "Author", "ISBN", "Why"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable recommendationsTable = new JTable(recommendationsModel);
    private final JTextArea insightsArea = new JTextArea();
    private final JLabel status = Ui.muted("Ready");

    public CatalogPanel(LibraryApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(Ui.BG);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        build();
    }

    private void build() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Ui.heading("Catalog Intelligence", 28));
        text.add(Box.createVerticalStrut(4));
        text.add(Ui.muted("Explainable recommendations and catalog analytics from your Java API."));
        header.add(text, BorderLayout.WEST);
        JButton refreshInsights = Ui.secondaryButton("Refresh Insights");
        refreshInsights.addActionListener(e -> refreshInsights());
        header.add(refreshInsights, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, 16, 0));
        content.setOpaque(false);

        JPanel recommendations = Ui.panel(new BorderLayout(0, 12));
        recommendations.setBorder(Ui.cardBorder());
        JPanel recTop = new JPanel();
        recTop.setOpaque(false);
        recTop.setLayout(new BoxLayout(recTop, BoxLayout.Y_AXIS));
        recTop.add(Ui.heading("Recommendations", 18));
        recTop.add(Box.createVerticalStrut(10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(query);
        controls.add(type);
        controls.add(limit);
        JButton recommend = Ui.primaryButton("Recommend");
        recommend.addActionListener(e -> refreshRecommendations());
        controls.add(recommend);
        recTop.add(controls);
        recommendations.add(recTop, BorderLayout.NORTH);
        Ui.styleTable(recommendationsTable);
        recommendationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        recommendationsTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        recommendationsTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        recommendationsTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        recommendationsTable.getColumnModel().getColumn(3).setPreferredWidth(145);
        recommendationsTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        recommendationsTable.getColumnModel().getColumn(5).setPreferredWidth(280);
        JScrollPane recScroll = new JScrollPane(recommendationsTable);
        recScroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        recommendations.add(recScroll, BorderLayout.CENTER);
        recommendations.add(status, BorderLayout.SOUTH);

        JPanel insights = Ui.panel(new BorderLayout(0, 12));
        insights.setBorder(Ui.cardBorder());
        insights.add(Ui.heading("Catalog insights", 18), BorderLayout.NORTH);
        insightsArea.setEditable(false);
        insightsArea.setLineWrap(true);
        insightsArea.setWrapStyleWord(true);
        insightsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        insightsArea.setForeground(Ui.TEXT);
        insightsArea.setBackground(Ui.SURFACE);
        insightsArea.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));
        JScrollPane insightScroll = new JScrollPane(insightsArea);
        insightScroll.setBorder(BorderFactory.createEmptyBorder());
        insights.add(insightScroll, BorderLayout.CENTER);

        content.add(recommendations);
        content.add(insights);
        add(content, BorderLayout.CENTER);
    }

    public void refreshAll() {
        refreshRecommendations();
        refreshInsights();
    }

    private void refreshRecommendations() {
        status.setText("Loading recommendations...");
        String q = query.getText();
        String preferred = String.valueOf(type.getSelectedItem());
        int max = (Integer) limit.getValue();
        runAsync(() -> api.getRecommendations(q, preferred, max), rows -> {
            recommendationsModel.setRowCount(0);
            for (Recommendation r : rows) {
                recommendationsModel.addRow(new Object[]{
                        r.score(), r.bookType(), r.title(), r.author(), r.isbnNo(), String.join("; ", r.reasons())
                });
            }
            status.setText(rows.size() + " recommendations");
        });
    }

    private void refreshInsights() {
        runAsync(api::getInsights, insights -> {
            StringBuilder text = new StringBuilder();
            text.append("CATALOG SUMMARY\n\n")
                    .append("Total books: ").append(insights.totalBooks()).append('\n')
                    .append("EBooks: ").append(insights.totalEBooks()).append('\n')
                    .append("Print books: ").append(insights.totalPrintBooks()).append('\n')
                    .append("Unique authors: ").append(insights.uniqueAuthors()).append("\n\n")
                    .append("AVERAGES\n\n")
                    .append(String.format("EBook size: %.1f KB%n", insights.averageEBookSizeKb()))
                    .append(String.format("Print pages: %.1f%n", insights.averagePrintPages()))
                    .append(String.format("Print weight: %.1f g%n%n", insights.averagePrintWeightGrams()))
                    .append("TOP AUTHORS\n\n");
            if (insights.topAuthors().isEmpty()) text.append("No author data available.\n");
            else {
                for (CatalogInsights.AuthorInsight author : insights.topAuthors()) {
                    text.append("• ").append(author.author()).append(" — ").append(author.bookCount()).append(" books\n");
                }
            }
            text.append("\nNEWEST BOOKS\n\n");
            if (insights.newestBooks().isEmpty()) text.append("No books available.\n");
            else insights.newestBooks().forEach(book -> text.append("• ").append(book.title()).append(" — ").append(book.author()).append('\n'));
            insightsArea.setText(text.toString());
            insightsArea.setCaretPosition(0);
        });
    }

    private <T> void runAsync(Callable<T> task, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return task.call(); }
            protected void done() {
                try { success.accept(get()); }
                catch (Exception ex) {
                    status.setText("Request failed");
                    JOptionPane.showMessageDialog(CatalogPanel.this, rootMessage(ex), "API error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
