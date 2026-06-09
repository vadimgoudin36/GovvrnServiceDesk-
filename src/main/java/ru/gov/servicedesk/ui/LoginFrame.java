package ru.gov.servicedesk.ui;

import ru.gov.servicedesk.model.User;
import ru.gov.servicedesk.service.AuthService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Полноэкранное окно авторизации.
 *
 * <p>После успешной проверки учетных данных открывает {@link MainFrame}.</p>
 */
public class LoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final JTextField loginField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);

    /**
     * Создает и настраивает окно входа.
     */
    public LoginFrame() {
        super("Авторизация - ServiceDesk");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(AppIcons.windowIcons());
        setSize(860, 650);
        setMinimumSize(getSize());

        JPanel page = UiTheme.pagePanel();
        page.setBorder(javax.swing.BorderFactory.createEmptyBorder(28, 34, 28, 34));
        add(page);

        JPanel card = UiTheme.surfacePanel();
        card.setPreferredSize(new Dimension(860, 640));
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);
        page.add(center, BorderLayout.CENTER);

        JPanel heading = new JPanel(new BorderLayout(4, 4));
        heading.setOpaque(false);
        JPanel titleBlock = new JPanel(new BorderLayout(4, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(UiTheme.title("ServiceDesk"), BorderLayout.NORTH);
        titleBlock.add(UiTheme.muted("Министерство региональной безопасности Воронежской области"), BorderLayout.CENTER);
        heading.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 18, 0));
        heading.add(AppIcons.logoLabel(132), BorderLayout.WEST);
        heading.add(titleBlock, BorderLayout.CENTER);
        card.add(heading, BorderLayout.NORTH);

        card.add(form(), BorderLayout.CENTER);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private JPanel form() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;

        UiTheme.styleField(loginField);
        UiTheme.styleField(passwordField);
        Dimension fieldSize = new Dimension(760, 48);
        loginField.setPreferredSize(fieldSize);
        passwordField.setPreferredSize(fieldSize);

        gbc.gridy = 0;
        form.add(new JLabel("Логин"), gbc);
        gbc.gridy = 1;
        form.add(loginField, gbc);

        gbc.gridy = 2;
        form.add(new JLabel("Пароль"), gbc);
        gbc.gridy = 3;
        form.add(passwordField, gbc);

        JButton loginButton = new JButton("Войти");
        UiTheme.stylePrimaryButton(loginButton);
        loginButton.setPreferredSize(new Dimension(760, 52));
        loginButton.addActionListener(event -> login());
        getRootPane().setDefaultButton(loginButton);

        gbc.gridy = 4;
        gbc.insets = new Insets(18, 0, 4, 0);
        form.add(loginButton, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(12, 0, 0, 0);
        form.add(UiTheme.muted("ИС учета и обработки заявок IT-поддержки"), gbc);
        return form;
    }

    private void login() {
        try {
            Optional<User> user = authService.login(
                    loginField.getText().trim(),
                    new String(passwordField.getPassword())
            );
            if (user.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Неверный логин или пароль", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new MainFrame(user.get()).setVisible(true);
            dispose();
        } catch (SQLException ex) {
            showError(this, ex);
        }
    }

    public static void showFatalError(Exception ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Критическая ошибка", JOptionPane.ERROR_MESSAGE);
    }

    static void showError(JFrame parent, Exception ex) {
        JOptionPane.showMessageDialog(parent, ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
