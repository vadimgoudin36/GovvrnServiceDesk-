package ru.gov.servicedesk;

import ru.gov.servicedesk.db.Database;
import ru.gov.servicedesk.service.RuleEngineService;
import ru.gov.servicedesk.ui.LoginFrame;
import ru.gov.servicedesk.ui.UiTheme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа desktop-приложения ServiceDesk.
 *
 * <p>Устанавливает визуальную тему, инициализирует SQLite, применяет
 * автоматические правила и открывает окно авторизации.</p>
 */
public class App {
    /**
     * Запускает приложение в потоке обработки событий Swing.
     *
     * @param args аргументы командной строки; приложением не используются
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UiTheme.install();
                Database.initialize();
                new RuleEngineService().applyAutomaticRules();
                new LoginFrame().setVisible(true);
            } catch (Exception ex) {
                LoginFrame.showFatalError(ex);
            }
        });
    }
}
