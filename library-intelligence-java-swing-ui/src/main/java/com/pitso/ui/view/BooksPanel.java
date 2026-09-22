package com.pitso.ui.view;

import com.pitso.ui.api.LibraryApiClient;
import com.pitso.ui.model.Book;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class BooksPanel extends JPanel {
    private final LibraryApiClient api;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Type", "Title", "Author", "ISBN", "Details", "Updated"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = Ui.textField(24);
    private final JComboBox<String> typeFilter = new JComboBox<>(new String[]{"All types", "EBOOK", "PRINTBOOK"});
    private final JLabel status = Ui.muted("Ready");
    private List<Book> currentBooks = new ArrayList<>();
    private Timer searchTimer;

    public BooksPanel(LibraryApiClient api) {
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
        text.add(Ui.heading("Books", 28));
        text.add(Box.createVerticalStrut(4));
        text.add(Ui.muted("Search, create, update and remove books from the catalog."));
        header.add(text, BorderLayout.WEST);
        JButton add = Ui.primaryButton("Add Book");
        add.addActionListener(e -> addBook());
        header.add(add, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel card = Ui.panel(new BorderLayout(0, 12));
        card.setBorder(Ui.cardBorder());
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        search.putClientProperty("JTextField.placeholderText", "Search title or author");
        left.add(search);
        typeFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        left.add(typeFilter);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton edit = Ui.secondaryButton("Edit");
        JButton delete = Ui.dangerButton("Delete");
        JButton refresh = Ui.secondaryButton("Refresh");
        edit.addActionListener(e -> editSelected());
        delete.addActionListener(e -> deleteSelected());
        refresh.addActionListener(e -> refresh());
        right.add(status);
        right.add(refresh);
        right.add(edit);
        right.add(delete);
        toolbar.add(right, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);

        Ui.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);
        table.getColumnModel().getColumn(5).setPreferredWidth(220);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER));
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        searchTimer = new Timer(450, e -> refresh());
        searchTimer.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { restartSearch(); }
            public void removeUpdate(DocumentEvent e) { restartSearch(); }
            public void changedUpdate(DocumentEvent e) { restartSearch(); }
        });
        typeFilter.addActionListener(e -> applyFilter());
    }

    private void restartSearch() {
        searchTimer.restart();
    }

    public void refresh() {
        status.setText("Loading...");
        String q = search.getText();
        runAsync(() -> api.searchBooks(q), books -> {
            currentBooks = books;
            applyFilter();
            status.setText(books.size() + " loaded");
        });
    }

    private void applyFilter() {
        String type = String.valueOf(typeFilter.getSelectedItem());
        model.setRowCount(0);
        for (Book b : currentBooks) {
            if (!"All types".equals(type) && !type.equalsIgnoreCase(b.bookType())) continue;
            model.addRow(new Object[]{
                    b.id(), b.bookType(), b.title(), b.author(), b.isbnNo(), b.sizeDetails(), compactDate(b.updatedAt())
            });
        }
    }

    private void addBook() {
        BookDialog dialog = new BookDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) return;
        runAsync(() -> api.createBook(dialog.getPayload()), ignored -> refresh());
    }

    private void editSelected() {
        Book book = selectedBook();
        if (book == null) {
            JOptionPane.showMessageDialog(this, "Select a book first.", "Edit book", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        BookDialog dialog = new BookDialog(SwingUtilities.getWindowAncestor(this), book);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) return;
        runAsync(() -> api.updateBook(book.id(), dialog.getPayload()), ignored -> refresh());
    }

    private void deleteSelected() {
        Book book = selectedBook();
        if (book == null) {
            JOptionPane.showMessageDialog(this, "Select a book first.", "Delete book", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + book.title() + "\"?\nThis action cannot be undone.",
                "Delete book", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        runAsync(() -> {
            api.deleteBook(book.id());
            return Boolean.TRUE;
        }, ignored -> refresh());
    }

    private Book selectedBook() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        long id = ((Number) model.getValueAt(modelRow, 0)).longValue();
        return currentBooks.stream().filter(b -> b.id() == id).findFirst().orElse(null);
    }

    private String compactDate(String value) {
        if (value == null) return "";
        return value.length() >= 16 ? value.substring(0, 16).replace('T', ' ') : value;
    }

    private <T> void runAsync(Callable<T> task, Consumer<T> success) {
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return task.call(); }
            protected void done() {
                try { success.accept(get()); }
                catch (Exception ex) {
                    status.setText("Request failed");
                    JOptionPane.showMessageDialog(BooksPanel.this, rootMessage(ex), "API error", JOptionPane.ERROR_MESSAGE);
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
