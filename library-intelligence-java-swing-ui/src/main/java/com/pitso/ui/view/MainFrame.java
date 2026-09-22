package com.pitso.ui.view;

import com.pitso.ui.api.LibraryApiClient;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    private final LibraryApiClient api;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final DashboardPanel dashboard;
    private final BooksPanel books;
    private final CatalogPanel catalog;
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel apiLabel = new JLabel();

    public MainFrame(LibraryApiClient api) {
        super("Library Intelligence");
        this.api = api;
        this.dashboard = new DashboardPanel(api);
        this.books = new BooksPanel(api);
        this.catalog = new CatalogPanel(api);
        build();
    }

    private void build() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1380, 820);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.BG);
        root.add(sidebar(), BorderLayout.WEST);

        content.setBackground(Ui.BG);
        content.add(dashboard, "dashboard");
        content.add(books, "books");
        content.add(catalog, "catalog");
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        showPage("dashboard");
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Ui.NAV);
        side.setPreferredSize(new Dimension(235, 0));
        side.setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Library Intelligence");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JLabel subtitle = new JLabel("Java Desktop Client");
        subtitle.setForeground(new Color(170, 185, 198));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brand.add(title);
        brand.add(Box.createVerticalStrut(3));
        brand.add(subtitle);
        side.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setBorder(BorderFactory.createEmptyBorder(34, 0, 0, 0));
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.add(navButton("dashboard", "Dashboard"));
        nav.add(Box.createVerticalStrut(8));
        nav.add(navButton("books", "Books"));
        nav.add(Box.createVerticalStrut(8));
        nav.add(navButton("catalog", "Catalog Intelligence"));
        nav.add(Box.createVerticalStrut(22));
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(65, 80, 95));
        nav.add(separator);
        nav.add(Box.createVerticalStrut(18));
        JButton apiSettings = navStyle("API Settings");
        apiSettings.addActionListener(e -> editApiUrl());
        nav.add(apiSettings);
        side.add(nav, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel connected = new JLabel("API endpoint");
        connected.setForeground(new Color(145, 162, 176));
        connected.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        apiLabel.setText(shortUrl(api.getBaseUrl()));
        apiLabel.setToolTipText(api.getBaseUrl());
        apiLabel.setForeground(new Color(206, 218, 227));
        apiLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottom.add(connected);
        bottom.add(Box.createVerticalStrut(3));
        bottom.add(apiLabel);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private JButton navButton(String page, String text) {
        JButton button = navStyle(text);
        navButtons.put(page, button);
        button.addActionListener(e -> showPage(page));
        return button;
    }

    private JButton navStyle(String text) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(200, 42));
        button.setForeground(new Color(222, 230, 237));
        button.setBackground(Ui.NAV);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void showPage(String page) {
        cards.show(content, page);
        navButtons.forEach((key, button) -> button.setBackground(key.equals(page) ? Ui.NAV_HOVER : Ui.NAV));
        switch (page) {
            case "dashboard" -> dashboard.refresh();
            case "books" -> books.refresh();
            case "catalog" -> catalog.refreshAll();
        }
    }

    private void editApiUrl() {
        String value = JOptionPane.showInputDialog(this,
                "API base URL\nExample: http://localhost:8080",
                api.getBaseUrl());
        if (value == null) return;
        try {
            api.setBaseUrl(value);
            apiLabel.setText(shortUrl(api.getBaseUrl()));
            apiLabel.setToolTipText(api.getBaseUrl());
            dashboard.refresh();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "API settings", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String shortUrl(String value) {
        if (value.length() <= 29) return value;
        return value.substring(0, 26) + "...";
    }
}
