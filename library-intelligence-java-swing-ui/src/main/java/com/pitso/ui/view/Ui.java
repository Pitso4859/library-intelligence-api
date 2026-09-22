package com.pitso.ui.view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class Ui {
    public static final Color BG = new Color(245, 247, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color NAV = new Color(20, 32, 45);
    public static final Color NAV_HOVER = new Color(35, 51, 67);
    public static final Color PRIMARY = new Color(34, 135, 190);
    public static final Color PRIMARY_DARK = new Color(24, 105, 151);
    public static final Color TEXT = new Color(35, 43, 51);
    public static final Color MUTED = new Color(104, 116, 128);
    public static final Color BORDER = new Color(222, 228, 234);
    public static final Color SUCCESS = new Color(36, 137, 82);
    public static final Color DANGER = new Color(181, 60, 60);

    private Ui() {}

    public static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        UIManager.put("control", SURFACE);
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusBlueGrey", new Color(232, 237, 242));
        UIManager.put("text", TEXT);
        UIManager.put("Table.alternateRowColor", new Color(249, 250, 252));
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(SURFACE);
        return panel;
    }

    public static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        return panel;
    }

    public static JLabel heading(String text, int size) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(new Font("Segoe UI", Font.BOLD, size));
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setForeground(TEXT);
        button.setBackground(SURFACE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = secondaryButton(text);
        button.setForeground(DANGER);
        return button;
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 20, 18, 20));
    }

    public static void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(220, 239, 250));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(247, 249, 251));
        table.getTableHeader().setReorderingAllowed(false);
    }
}
