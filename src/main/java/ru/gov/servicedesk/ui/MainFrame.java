package ru.gov.servicedesk.ui;

import ru.gov.servicedesk.dao.CategoryDao;
import ru.gov.servicedesk.dao.CommentDao;
import ru.gov.servicedesk.dao.ActionLogDao;
import ru.gov.servicedesk.dao.TicketDao;
import ru.gov.servicedesk.dao.UserDao;
import ru.gov.servicedesk.model.ActionLog;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.Ticket;
import ru.gov.servicedesk.model.TicketComment;
import ru.gov.servicedesk.model.TicketStatus;
import ru.gov.servicedesk.model.User;
import ru.gov.servicedesk.service.RuleEngineService;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {
    private final User currentUser;
    private final TicketDao ticketDao = new TicketDao();
    private final CommentDao commentDao = new CommentDao();
    private final ActionLogDao actionLogDao = new ActionLogDao();
    private final UserDao userDao = new UserDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final RuleEngineService ruleEngineService = new RuleEngineService();
    private javax.swing.Timer ruleTimer;

    private final DefaultTableModel ticketModel = readonlyModel("ID", "Тема", "Категория", "Статус", "Автор", "Исполнитель", "Создана", "Обновлена");
    private final JTable ticketTable = new JTable(ticketModel);
    private final JTextArea ticketDetails = new JTextArea();
    private final JTextArea commentsArea = new JTextArea();
    private final JTextField commentField = new JTextField(40);

    public MainFrame(User currentUser) {
        super("ServiceDesk МРБ Воронежской области - " + currentUser.getRole().getTitle());
        this.currentUser = currentUser;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(AppIcons.windowIcons());
        setSize(1180, 760);
        setMinimumSize(getSize());

        JPanel page = UiTheme.pagePanel();
        page.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 16, 16, 16));
        setContentPane(page);
        page.add(header(), BorderLayout.NORTH);
        page.add(content(), BorderLayout.CENTER);
        refreshAll();
        startRuleTimer();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private JPanel header() {
        JPanel panel = UiTheme.surfacePanel();

        JPanel text = new JPanel(new BorderLayout(4, 4));
        text.setOpaque(false);
        text.add(UiTheme.title("ServiceDesk Министерства региональной безопасности Воронежской области"), BorderLayout.NORTH);
        text.add(UiTheme.muted(currentUser.getFullName() + " | " + currentUser.getRole().getTitle()), BorderLayout.CENTER);
        JPanel brand = new JPanel(new BorderLayout(0, 0));
        brand.setOpaque(false);
        brand.add(AppIcons.logoLabel(76), BorderLayout.WEST);
        brand.add(text, BorderLayout.CENTER);
        panel.add(brand, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton refreshButton = new JButton("Обновить");
        UiTheme.styleButton(refreshButton);
        refreshButton.addActionListener(event -> refreshAll());
        JButton logoutButton = new JButton("Выйти");
        UiTheme.styleButton(logoutButton);
        logoutButton.addActionListener(event -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        buttons.add(refreshButton);
        buttons.add(logoutButton);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    private JTabbedPane content() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Заявки", ticketsPanel());
        if (currentUser.getRole() == Role.EMPLOYEE) {
            tabs.addTab("Создать заявку", createTicketPanel());
        }
        if (currentUser.getRole() == Role.SPECIALIST) {
            tabs.addTab("Работа с заявкой", specialistActionsPanel());
        }
        if (currentUser.getRole() == Role.ADMIN) {
            tabs.addTab("Назначение", adminAssignmentPanel());
            tabs.addTab("Пользователи", usersPanel());
            tabs.addTab("Статистика", statsPanel());
            tabs.addTab("Журнал действий", actionLogPanel());
        }
        return tabs;
    }

    private JPanel ticketsPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));

        setupTicketTable();
        JScrollPane tableScroll = new JScrollPane(ticketTable);
        UiTheme.styleScrollPane(tableScroll);

        JPanel bottom = UiTheme.pagePanel();
        ticketDetails.setEditable(false);
        ticketDetails.setRows(5);
        UiTheme.styleTextArea(ticketDetails);
        commentsArea.setEditable(false);
        commentsArea.setRows(7);
        UiTheme.styleTextArea(commentsArea);

        JScrollPane detailsScroll = new JScrollPane(ticketDetails);
        JScrollPane commentsScroll = new JScrollPane(commentsArea);
        UiTheme.styleScrollPane(detailsScroll);
        UiTheme.styleScrollPane(commentsScroll);

        bottom.add(UiTheme.section("Описание заявки", detailsScroll), BorderLayout.NORTH);
        bottom.add(UiTheme.section("Комментарии", commentsScroll), BorderLayout.CENTER);
        bottom.add(commentPanel(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, bottom);
        splitPane.setResizeWeight(0.58);
        splitPane.setBorder(null);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void setupTicketTable() {
        UiTheme.styleTable(ticketTable);
        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ticketTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        ticketTable.getColumnModel().getColumn(1).setPreferredWidth(230);
        ticketTable.getColumnModel().getColumn(3).setCellRenderer(UiTheme.statusRenderer());
        ticketTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedTicketDetails();
            }
        });
    }

    private JPanel commentPanel() {
        JPanel panel = UiTheme.surfacePanel();
        JLabel label = new JLabel("Новый комментарий");
        panel.add(label, BorderLayout.WEST);

        UiTheme.styleField(commentField);
        panel.add(commentField, BorderLayout.CENTER);

        JButton addButton = new JButton("Добавить");
        UiTheme.stylePrimaryButton(addButton);
        addButton.addActionListener(event -> addComment());
        panel.add(addButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel createTicketPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel form = UiTheme.surfacePanel();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = formConstraints();

        JTextField titleField = new JTextField(38);
        UiTheme.styleField(titleField);
        JComboBox<String> categoryBox = new JComboBox<>(loadCategories().toArray(String[]::new));
        UiTheme.styleCombo(categoryBox);
        JTextArea descriptionArea = new JTextArea(9, 38);
        UiTheme.styleTextArea(descriptionArea);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        UiTheme.styleScrollPane(descriptionScroll);
        JButton createButton = new JButton("Создать заявку");
        UiTheme.stylePrimaryButton(createButton);

        addFormRow(form, gbc, 0, "Тема", titleField);
        addFormRow(form, gbc, 1, "Категория", categoryBox);
        addFormRow(form, gbc, 2, "Описание", descriptionScroll);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        form.add(createButton, gbc);

        createButton.addActionListener(event -> {
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            if (title.isBlank() || description.isBlank()) {
                message("Заполните тему и описание заявки");
                return;
            }
            try {
                Ticket ticket = new Ticket();
                ticket.setTitle(title);
                ticket.setDescription(description);
                ticket.setCategory(String.valueOf(categoryBox.getSelectedItem()));
                ticket.setAuthorId(currentUser.getId());
                int ticketId = ticketDao.create(ticket);
                ruleEngineService.applyAutomaticRulesForTicket(ticketId);
                titleField.setText("");
                descriptionArea.setText("");
                refreshAll();
                message("Заявка создана");
            } catch (SQLException ex) {
                LoginFrame.showError(this, ex);
            }
        });

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    private JPanel specialistActionsPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel actions = UiTheme.surfacePanel();
        actions.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        JButton assignButton = new JButton("Взять в работу");
        JButton statusButton = new JButton("Сменить статус");
        JButton closeButton = new JButton("Закрыть заявку");
        JButton doneButton = new JButton("Отметить выполненной");
        JComboBox<TicketStatus> statusBox = new JComboBox<>(TicketStatus.values());
        JTextArea reportArea = new JTextArea(8, 64);
        UiTheme.styleTextArea(reportArea);
        JScrollPane reportScroll = new JScrollPane(reportArea);
        UiTheme.styleScrollPane(reportScroll);

        UiTheme.stylePrimaryButton(assignButton);
        UiTheme.styleButton(statusButton);
        UiTheme.styleButton(closeButton);
        UiTheme.stylePrimaryButton(doneButton);
        UiTheme.styleCombo(statusBox);

        assignButton.addActionListener(event -> updateSelectedTicket(ticketId -> ticketDao.assignTo(ticketId, currentUser.getId(), currentUser.getId())));
        statusButton.addActionListener(event -> updateSelectedTicket(ticketId -> ticketDao.updateStatus(ticketId, (TicketStatus) statusBox.getSelectedItem(), currentUser.getId())));
        closeButton.addActionListener(event -> updateSelectedTicket(ticketId -> ticketDao.updateStatus(ticketId, TicketStatus.CLOSED, currentUser.getId())));
        doneButton.addActionListener(event -> {
            String report = reportArea.getText().trim();
            if (report.isBlank()) {
                message("Введите отчет о выполнении");
                return;
            }
            updateSelectedTicket(ticketId -> ticketDao.completeWithReport(ticketId, report, currentUser.getId()));
            reportArea.setText("");
        });

        actions.add(assignButton);
        actions.add(new JLabel("Новый статус"));
        actions.add(statusBox);
        actions.add(statusButton);
        actions.add(closeButton);
        panel.add(actions, BorderLayout.NORTH);

        JPanel reportPanel = UiTheme.section("Отчет о выполнении", reportScroll);
        JPanel reportFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        reportFooter.setOpaque(false);
        reportFooter.add(doneButton);
        reportPanel.add(reportFooter, BorderLayout.SOUTH);
        panel.add(reportPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel adminAssignmentPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JPanel actions = UiTheme.surfacePanel();
        actions.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        JComboBox<User> specialistsBox = new JComboBox<>();
        JButton reloadButton = new JButton("Обновить специалистов");
        JButton assignButton = new JButton("Назначить выбранную заявку");

        UiTheme.styleCombo(specialistsBox);
        UiTheme.styleButton(reloadButton);
        UiTheme.stylePrimaryButton(assignButton);

        Runnable loadSpecialists = () -> {
            try {
                specialistsBox.removeAllItems();
                for (User user : userDao.findByRole(Role.SPECIALIST)) {
                    specialistsBox.addItem(user);
                }
            } catch (SQLException ex) {
                LoginFrame.showError(this, ex);
            }
        };

        reloadButton.addActionListener(event -> loadSpecialists.run());
        assignButton.addActionListener(event -> {
            User specialist = (User) specialistsBox.getSelectedItem();
            if (specialist == null) {
                message("Нет доступных IT-специалистов");
                return;
            }
            updateSelectedTicket(ticketId -> ticketDao.assignTo(ticketId, specialist.getId(), currentUser.getId()));
            message("Заявка назначена: " + specialist.getFullName());
        });

        actions.add(new JLabel("IT-специалист"));
        actions.add(specialistsBox);
        actions.add(assignButton);
        actions.add(reloadButton);

        JTextArea hint = new JTextArea("""
                1. Откройте вкладку «Заявки» и выберите нужную заявку в таблице.
                2. Вернитесь на эту вкладку.
                3. Выберите IT-специалиста и нажмите «Назначить выбранную заявку».

                После назначения заявка перейдет в статус «В работе», а исполнитель появится в таблице заявок.
                """);
        hint.setEditable(false);
        UiTheme.styleTextArea(hint);

        panel.add(actions, BorderLayout.NORTH);
        panel.add(UiTheme.section("Порядок назначения", hint), BorderLayout.CENTER);
        loadSpecialists.run();
        return panel;
    }

    private JPanel usersPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));

        DefaultTableModel usersModel = readonlyModel("ID", "Логин", "ФИО", "Роль");
        JTable usersTable = new JTable(usersModel);
        UiTheme.styleTable(usersTable);
        JScrollPane usersScroll = new JScrollPane(usersTable);
        UiTheme.styleScrollPane(usersScroll);

        JPanel form = UiTheme.surfacePanel();
        form.setLayout(new BorderLayout(12, 12));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JTextField loginField = new JTextField();
        JTextField passwordField = new JTextField();
        JTextField fullNameField = new JTextField();
        JComboBox<Role> roleBox = new JComboBox<>(Role.values());
        JButton createButton = new JButton("Добавить");
        JButton updateButton = new JButton("Сохранить изменения");

        UiTheme.styleField(loginField);
        UiTheme.styleField(passwordField);
        UiTheme.styleField(fullNameField);
        UiTheme.styleCombo(roleBox);
        setFieldWidth(loginField, 190);
        setFieldWidth(passwordField, 190);
        setFieldWidth(fullNameField, 430);
        roleBox.setPreferredSize(new Dimension(180, roleBox.getPreferredSize().height));
        roleBox.setMinimumSize(roleBox.getPreferredSize());
        UiTheme.stylePrimaryButton(createButton);
        UiTheme.styleButton(updateButton);

        addUserField(fields, 0, "Логин", loginField);
        addUserField(fields, 1, "Пароль", passwordField);
        addUserField(fields, 2, "ФИО", fullNameField);
        addUserField(fields, 3, "Роль", roleBox);
        buttons.add(createButton);
        buttons.add(updateButton);
        form.add(fields, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);

        Runnable loadUsers = () -> {
            try {
                usersModel.setRowCount(0);
                for (User user : userDao.findAll()) {
                    usersModel.addRow(new Object[]{user.getId(), user.getLogin(), user.getFullName(), user.getRole()});
                }
            } catch (SQLException ex) {
                LoginFrame.showError(this, ex);
            }
        };

        usersTable.getSelectionModel().addListSelectionListener(event -> {
            int row = usersTable.getSelectedRow();
            if (!event.getValueIsAdjusting() && row >= 0) {
                loginField.setText(String.valueOf(usersModel.getValueAt(row, 1)));
                passwordField.setText("");
                fullNameField.setText(String.valueOf(usersModel.getValueAt(row, 2)));
                Object role = usersModel.getValueAt(row, 3);
                if (role instanceof Role selectedRole) {
                    roleBox.setSelectedItem(selectedRole);
                }
            }
        });

        createButton.addActionListener(event -> {
            try {
                validateUserForm(loginField, passwordField, fullNameField);
                userDao.create(loginField.getText().trim(), passwordField.getText().trim(), fullNameField.getText().trim(), (Role) roleBox.getSelectedItem());
                clearUserForm(loginField, passwordField, fullNameField);
                loadUsers.run();
                message("Пользователь добавлен");
            } catch (Exception ex) {
                LoginFrame.showError(this, ex);
            }
        });

        updateButton.addActionListener(event -> {
            int row = usersTable.getSelectedRow();
            if (row < 0) {
                message("Выберите пользователя");
                return;
            }
            try {
                validateUserEditForm(loginField, fullNameField);
                String password = passwordField.getText().trim();
                if (password.isBlank()) {
                    password = userDao.findByLogin(String.valueOf(usersModel.getValueAt(row, 1))).orElseThrow().getPassword();
                }
                userDao.update((int) usersModel.getValueAt(row, 0), loginField.getText().trim(), password, fullNameField.getText().trim(), (Role) roleBox.getSelectedItem());
                clearUserForm(loginField, passwordField, fullNameField);
                loadUsers.run();
                refreshAll();
                message("Пользователь обновлен");
            } catch (Exception ex) {
                LoginFrame.showError(this, ex);
            }
        });

        panel.add(usersScroll, BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        loadUsers.run();
        return panel;
    }

    private JPanel statsPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 0, 0, 0));

        JTextArea statsArea = new JTextArea(18, 60);
        statsArea.setEditable(false);
        UiTheme.styleTextArea(statsArea);
        JScrollPane statsScroll = new JScrollPane(statsArea);
        UiTheme.styleScrollPane(statsScroll);
        JButton refreshButton = new JButton("Обновить статистику");
        UiTheme.stylePrimaryButton(refreshButton);

        Runnable loadStats = () -> {
            try {
                StringBuilder text = new StringBuilder();
                appendStats(text, "По статусам", ticketDao.countByStatus());
                appendStats(text, "\nПо категориям", ticketDao.countByCategory());
                statsArea.setText(text.toString());
            } catch (SQLException ex) {
                LoginFrame.showError(this, ex);
            }
        };
        refreshButton.addActionListener(event -> loadStats.run());

        JPanel top = UiTheme.surfacePanel();
        top.add(refreshButton, BorderLayout.WEST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(UiTheme.section("Сводка по заявкам", statsScroll), BorderLayout.CENTER);
        loadStats.run();
        return panel;
    }

    private JPanel actionLogPanel() {
        JPanel panel = UiTheme.pagePanel();
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 0, 0, 0));

        DefaultTableModel logModel = readonlyModel("Время", "Заявка", "Пользователь", "Действие", "Было", "Стало", "Описание");
        JTable logTable = new JTable(logModel);
        UiTheme.styleTable(logTable);
        JScrollPane logScroll = new JScrollPane(logTable);
        UiTheme.styleScrollPane(logScroll);

        JButton refreshButton = new JButton("Обновить журнал");
        UiTheme.stylePrimaryButton(refreshButton);

        Runnable loadLogs = () -> {
            try {
                logModel.setRowCount(0);
                for (ActionLog log : actionLogDao.findRecent(300)) {
                    logModel.addRow(new Object[]{
                            log.getCreatedAt(),
                            log.getTicketId() == null ? "-" : log.getTicketId(),
                            log.getUserName() == null ? "Система" : log.getUserName(),
                            actionTitle(log.getActionType()),
                            statusTitle(log.getOldStatus()),
                            statusTitle(log.getNewStatus()),
                            log.getDescription()
                    });
                }
            } catch (SQLException ex) {
                LoginFrame.showError(this, ex);
            }
        };
        refreshButton.addActionListener(event -> loadLogs.run());

        JPanel top = UiTheme.surfacePanel();
        top.add(refreshButton, BorderLayout.WEST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(UiTheme.section("Журнал автоматических и пользовательских действий", logScroll), BorderLayout.CENTER);
        loadLogs.run();
        return panel;
    }

    private void loadTickets() {
        try {
            List<Ticket> tickets = currentUser.getRole() == Role.EMPLOYEE
                    ? ticketDao.findByAuthor(currentUser.getId())
                    : ticketDao.findAll();
            ticketModel.setRowCount(0);
            for (Ticket ticket : tickets) {
                ticketModel.addRow(new Object[]{
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getCategory(),
                        ticket.getStatus(),
                        ticket.getAuthorName(),
                        ticket.getExecutorName() == null ? "-" : ticket.getExecutorName(),
                        ticket.getCreatedAt(),
                        ticket.getUpdatedAt()
                });
            }
            if (ticketModel.getRowCount() > 0) {
                ticketTable.setRowSelectionInterval(0, 0);
            } else {
                ticketDetails.setText("");
                commentsArea.setText("");
            }
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
        }
    }

    private void showSelectedTicketDetails() {
        Ticket ticket = selectedTicket();
        if (ticket == null) {
            return;
        }
        ticketDetails.setText("Тема: " + ticket.getTitle()
                + "\nКатегория: " + ticket.getCategory()
                + "\nСтатус: " + ticket.getStatus().getTitle()
                + "\nАвтор: " + ticket.getAuthorName()
                + "\nИсполнитель: " + (ticket.getExecutorName() == null ? "-" : ticket.getExecutorName())
                + "\nСоздана: " + ticket.getCreatedAt()
                + "\nОбновлена: " + ticket.getUpdatedAt()
                + "\n\nОписание:\n" + ticket.getDescription()
                + resolutionText(ticket));
        try {
            StringBuilder text = new StringBuilder();
            for (TicketComment comment : commentDao.findByTicket(ticket.getId())) {
                text.append(comment.getCreatedAt())
                        .append(" | ")
                        .append(comment.getAuthorName())
                        .append("\n")
                        .append(comment.getText())
                        .append("\n\n");
            }
            commentsArea.setText(text.toString());
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
        }
    }

    private Ticket selectedTicket() {
        int row = ticketTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int ticketId = (int) ticketModel.getValueAt(row, 0);
        try {
            return ticketDao.findAll().stream()
                    .filter(ticket -> ticket.getId() == ticketId)
                    .findFirst()
                    .orElse(null);
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
            return null;
        }
    }

    private void addComment() {
        Ticket ticket = selectedTicket();
        if (ticket == null) {
            message("Выберите заявку");
            return;
        }
        String text = commentField.getText().trim();
        if (text.isBlank()) {
            message("Введите текст комментария");
            return;
        }
        try {
            commentDao.create(ticket.getId(), currentUser.getId(), text);
            ruleEngineService.applyAutomaticRulesForTicket(ticket.getId());
            commentField.setText("");
            refreshAll();
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
        }
    }

    private void updateSelectedTicket(TicketAction action) {
        Ticket ticket = selectedTicket();
        if (ticket == null) {
            message("Выберите заявку");
            return;
        }
        try {
            action.apply(ticket.getId());
            refreshAll();
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
        }
    }

    private String resolutionText(Ticket ticket) {
        String report = ticket.getResolutionReport();
        if (report == null || report.isBlank()) {
            if (ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.CLOSED) {
                return "\n\nОтчет о выполнении:\nОтчет пока не заполнен.";
            }
            return "";
        }
        return "\n\nОтчет о выполнении:\n" + report;
    }

    private void refreshAll() {
        try {
            ruleEngineService.applyAutomaticRules();
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
        }
        loadTickets();
    }

    private void startRuleTimer() {
        ruleTimer = new javax.swing.Timer(60_000, event -> refreshAll());
        ruleTimer.start();
    }

    private List<String> loadCategories() {
        try {
            return categoryDao.findAllNames();
        } catch (SQLException ex) {
            LoginFrame.showError(this, ex);
            return List.of("Другое");
        }
    }

    private DefaultTableModel readonlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private GridBagConstraints formConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);
    }

    private void addUserField(JPanel panel, int column, String label, java.awt.Component component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = column;
        gbc.gridy = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(component, gbc);
    }

    private void setFieldWidth(JTextField field, int width) {
        Dimension size = new Dimension(width, field.getPreferredSize().height);
        field.setPreferredSize(size);
        field.setMinimumSize(size);
    }

    private void validateUserForm(JTextField loginField, JTextField passwordField, JTextField fullNameField) {
        if (loginField.getText().trim().isBlank() || passwordField.getText().trim().isBlank() || fullNameField.getText().trim().isBlank()) {
            throw new IllegalArgumentException("Заполните логин, пароль и ФИО");
        }
    }

    private void validateUserEditForm(JTextField loginField, JTextField fullNameField) {
        if (loginField.getText().trim().isBlank() || fullNameField.getText().trim().isBlank()) {
            throw new IllegalArgumentException("Заполните логин и ФИО");
        }
    }

    private void clearUserForm(JTextField loginField, JTextField passwordField, JTextField fullNameField) {
        loginField.setText("");
        passwordField.setText("");
        fullNameField.setText("");
    }

    private void appendStats(StringBuilder text, String title, Map<String, Integer> stats) {
        text.append(title).append("\n");
        if (stats.isEmpty()) {
            text.append("Нет данных\n");
            return;
        }
        stats.forEach((key, value) -> text.append(displayStatKey(key)).append(": ").append(value).append("\n"));
    }

    private String displayStatKey(String key) {
        try {
            return TicketStatus.valueOf(key).getTitle();
        } catch (IllegalArgumentException ex) {
            return key;
        }
    }

    private String statusTitle(String statusName) {
        if (statusName == null || statusName.isBlank()) {
            return "-";
        }
        try {
            return TicketStatus.valueOf(statusName).getTitle();
        } catch (IllegalArgumentException ex) {
            return statusName;
        }
    }

    private String actionTitle(String actionType) {
        return switch (actionType) {
            case "ASSIGN" -> "Назначение";
            case "STATUS_CHANGE" -> "Смена статуса";
            case "AUTO_STATUS_CHANGE" -> "Автоизменение статуса";
            case "COMPLETE" -> "Отчет о выполнении";
            default -> actionType;
        };
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(this, text);
    }

    @FunctionalInterface
    private interface TicketAction {
        void apply(int ticketId) throws SQLException;
    }
}
