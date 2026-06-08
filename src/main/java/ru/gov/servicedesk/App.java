package ru.gov.servicedesk;

import ru.gov.servicedesk.db.Database;
import ru.gov.servicedesk.service.RuleEngineService;
import ru.gov.servicedesk.ui.LoginFrame;
import ru.gov.servicedesk.ui.UiTheme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class App {
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
