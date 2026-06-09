package ru.gov.servicedesk.ui;

import ru.gov.servicedesk.model.TicketStatus;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

/**
 * Единая визуальная тема и фабрика оформленных Swing-компонентов.
 */
public final class UiTheme {
    public static final Color BACKGROUND = new Color(244, 247, 251);
    public static final Color SURFACE = Color.WHITE;
    public static final Color PRIMARY = new Color(28, 89, 164);
    public static final Color PRIMARY_DARK = new Color(18, 67, 128);
    public static final Color TEXT = new Color(30, 41, 59);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color BORDER = new Color(214, 222, 235);

    private static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 15);

    private UiTheme() {
    }

    /**
     * Устанавливает Nimbus и общие шрифты и цвета приложения.
     *
     * @throws Exception если Look and Feel установить не удалось
     */
    public static void install() throws Exception {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
        UIManager.put("control", BACKGROUND);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusBase", PRIMARY);
        UIManager.put("nimbusBlueGrey", BORDER);
        UIManager.put("defaultFont", BASE_FONT);
        UIManager.put("Label.font", BASE_FONT);
        UIManager.put("Button.font", BASE_FONT);
        UIManager.put("TextField.font", BASE_FONT);
        UIManager.put("PasswordField.font", BASE_FONT);
        UIManager.put("TextArea.font", BASE_FONT);
        UIManager.put("ComboBox.font", BASE_FONT);
        UIManager.put("Table.font", BASE_FONT);
        UIManager.put("TableHeader.font", BASE_FONT.deriveFont(Font.BOLD));
        UIManager.put("TabbedPane.font", BASE_FONT.deriveFont(Font.BOLD));
    }

    public static JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BACKGROUND);
        return panel;
    }

    public static JPanel surfacePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(SURFACE);
        panel.setBorder(compound(14));
        return panel;
    }

    public static JPanel section(String title, Component content) {
        JPanel panel = surfacePanel();
        JLabel label = new JLabel(title);
        label.setFont(SECTION_FONT);
        label.setForeground(TEXT);
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    public static void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 16, 8, 16));
    }

    public static void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 14, 8, 14));
    }

    public static void styleField(JTextField field) {
        field.setBorder(compound(8));
    }

    public static void styleCombo(JComboBox<?> comboBox) {
        comboBox.setBackground(SURFACE);
    }

    public static void styleTextArea(JTextArea area) {
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        area.setForeground(TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(232, 238, 247));
        table.setSelectionBackground(new Color(221, 235, 255));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(232, 238, 247));
        table.getTableHeader().setForeground(TEXT);
    }

    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof TicketStatus status) {
                    setText(status.getTitle());
                    if (!isSelected) {
                        component.setBackground(statusBackground(status));
                        component.setForeground(statusForeground(status));
                    }
                } else if (!isSelected) {
                    component.setBackground(SURFACE);
                    component.setForeground(TEXT);
                }
                setHorizontalAlignment(CENTER);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return component;
            }
        };
    }

    private static Color statusBackground(TicketStatus status) {
        return switch (status) {
            case NEW -> new Color(232, 244, 255);
            case IN_PROGRESS -> new Color(255, 247, 214);
            case DONE -> new Color(224, 248, 232);
            case CLOSED -> new Color(235, 238, 244);
            case OVERDUE -> new Color(255, 226, 226);
            case URGENT_REVIEW -> new Color(255, 235, 204);
            case ESCALATION -> new Color(240, 232, 255);
        };
    }

    private static Color statusForeground(TicketStatus status) {
        return switch (status) {
            case NEW -> new Color(23, 84, 142);
            case IN_PROGRESS -> new Color(130, 87, 0);
            case DONE -> new Color(25, 103, 55);
            case CLOSED -> new Color(71, 85, 105);
            case OVERDUE -> new Color(153, 27, 27);
            case URGENT_REVIEW -> new Color(146, 64, 14);
            case ESCALATION -> new Color(91, 33, 182);
        };
    }

    private static Border compound(int padding) {
        return new CompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding)
        );
    }
}
