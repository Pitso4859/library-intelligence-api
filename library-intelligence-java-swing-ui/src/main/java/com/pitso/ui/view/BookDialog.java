package com.pitso.ui.view;

import com.pitso.ui.model.Book;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class BookDialog extends JDialog {
    private final JTextField titleField = Ui.textField(24);
    private final JTextField authorField = Ui.textField(24);
    private final JTextField isbnField = Ui.textField(24);
    private final JTextField fileSizeField = Ui.textField(12);
    private final JTextField pagesField = Ui.textField(12);
    private final JTextField weightField = Ui.textField(12);
    private final JLabel typeHint = Ui.muted("ISBN starting with 0 = EBook, 1 = PrintBook");
    private boolean confirmed;
    private final Book existing;

    public BookDialog(Window owner, Book existing) {
        super(owner, existing == null ? "Add Book" : "Edit Book", ModalityType.APPLICATION_MODAL);
        this.existing = existing;
        build();
        if (existing != null) load(existing);
        updateTypeFields();
        pack();
        setMinimumSize(new Dimension(520, 520));
        setLocationRelativeTo(owner);
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(Ui.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(Ui.heading(existing == null ? "Add a new book" : "Edit book", 22));
        header.add(Box.createVerticalStrut(4));
        header.add(Ui.muted(existing == null
                ? "Enter the book details. The ISBN prefix determines the book type."
                : "Update the fields below. ISBN and book type cannot be changed."));
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 12);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        int row = 0;
        addRow(form, c, row++, "Title", titleField);
        addRow(form, c, row++, "Author", authorField);
        addRow(form, c, row++, "ISBN", isbnField);

        c.gridx = 1;
        c.gridy = row++;
        c.weightx = 1;
        form.add(typeHint, c);
        addRow(form, c, row++, "File size (KB)", fileSizeField);
        addRow(form, c, row++, "Pages", pagesField);
        addRow(form, c, row++, "Weight (g)", weightField);

        isbnField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateTypeFields(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateTypeFields(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateTypeFields(); }
        });
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton cancel = Ui.secondaryButton("Cancel");
        JButton save = Ui.primaryButton(existing == null ? "Add Book" : "Save Changes");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            try {
                validateInput();
                confirmed = true;
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Check book details", JOptionPane.WARNING_MESSAGE);
            }
        });
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(save);
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, JComponent component) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(Ui.MUTED);
        panel.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(component, c);
    }

    private void load(Book book) {
        titleField.setText(book.title());
        authorField.setText(book.author());
        isbnField.setText(book.isbnNo());
        isbnField.setEnabled(false);
        if (book.fileSizeKb() != null) fileSizeField.setText(String.valueOf(book.fileSizeKb()));
        if (book.noOfPages() != null) pagesField.setText(String.valueOf(book.noOfPages()));
        if (book.weightGrams() != null) weightField.setText(String.valueOf(book.weightGrams()));
    }

    private void updateTypeFields() {
        boolean ebook;
        if (existing != null) ebook = existing.isEBook();
        else ebook = isbnField.getText().trim().startsWith("0");
        boolean print = existing != null ? existing.isPrintBook() : isbnField.getText().trim().startsWith("1");
        fileSizeField.setEnabled(ebook);
        pagesField.setEnabled(print);
        weightField.setEnabled(print);
        typeHint.setText(ebook ? "Detected type: EBook" : print ? "Detected type: PrintBook" : "ISBN starting with 0 = EBook, 1 = PrintBook");
    }

    private void validateInput() {
        if (titleField.getText().isBlank()) throw new IllegalArgumentException("Title is required.");
        if (authorField.getText().isBlank()) throw new IllegalArgumentException("Author is required.");
        if (existing == null) {
            String isbn = isbnField.getText().trim();
            if (isbn.length() != 10) throw new IllegalArgumentException("ISBN must be exactly 10 characters.");
            if (!isbn.startsWith("0") && !isbn.startsWith("1")) throw new IllegalArgumentException("ISBN must start with 0 or 1.");
        }
        if (isEBook()) parsePositiveInt(fileSizeField.getText(), "File size");
        if (isPrintBook()) {
            parsePositiveInt(pagesField.getText(), "Number of pages");
            parsePositiveDouble(weightField.getText(), "Weight");
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Map<String, Object> getPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", titleField.getText().trim());
        payload.put("author", authorField.getText().trim());
        if (existing == null) payload.put("isbnNo", isbnField.getText().trim());
        if (isEBook()) {
            payload.put("fileSizeKb", parsePositiveInt(fileSizeField.getText(), "File size"));
        } else if (isPrintBook()) {
            payload.put("noOfPages", parsePositiveInt(pagesField.getText(), "Number of pages"));
            payload.put("weightGrams", parsePositiveDouble(weightField.getText(), "Weight"));
        }
        return payload;
    }

    private boolean isEBook() {
        return existing != null ? existing.isEBook() : isbnField.getText().trim().startsWith("0");
    }

    private boolean isPrintBook() {
        return existing != null ? existing.isPrintBook() : isbnField.getText().trim().startsWith("1");
    }

    private int parsePositiveInt(String text, String label) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 1) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a positive whole number.");
        }
    }

    private double parsePositiveDouble(String text, String label) {
        try {
            double value = Double.parseDouble(text.trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a number greater than zero.");
        }
    }
}
