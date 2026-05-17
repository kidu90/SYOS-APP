package com.syos.presentation.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import com.syos.domain.entity.User;

public class AuthDialog extends JDialog {
    private final SYOSGuiApplication app;
    private User authenticatedUser;

    public AuthDialog(JFrame owner, SYOSGuiApplication app) {
        super(owner, "SYOS Authentication", true);
        this.app = app;
        buildUi();
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Login", buildLoginPanel(tabs));
        tabs.addTab("Sign Up", buildRegisterPanel(tabs));
        add(tabs, BorderLayout.CENTER);
        setSize(560, 360);
        setLocationRelativeTo(getOwner());
    }

    private JPanel buildLoginPanel(JTabbedPane tabs) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Username"));
        panel.add(usernameField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton switchButton = new JButton("Go to Register");
        panel.add(loginButton);
        panel.add(switchButton);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isBlank() || password.isBlank()) {
                app.showError("Login Error", "All fields are required");
                return;
            }
            loginButton.setEnabled(false);
            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() {
                    return app.getUserService().login(username, password);
                }

                @Override
                protected void done() {
                    loginButton.setEnabled(true);
                    try {
                        authenticatedUser = get();
                        app.showSuccess("Login Successful", "Welcome, " + authenticatedUser.getFullName() + "!");
                        dispose();
                    } catch (Exception ex) {
                        app.showError("Login Error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        });

        switchButton.addActionListener(e -> tabs.setSelectedIndex(1));
        return panel;
    }

    private JPanel buildRegisterPanel(JTabbedPane tabs) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField fullNameField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JTextField addressField = new JTextField();

        panel.add(new JLabel("Full Name"));
        panel.add(fullNameField);
        panel.add(new JLabel("Username"));
        panel.add(usernameField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);
        panel.add(new JLabel("Delivery Address"));
        panel.add(addressField);

        JButton registerButton = new JButton("Register");
        JButton switchButton = new JButton("Go to Login");
        panel.add(registerButton);
        panel.add(switchButton);

        registerButton.addActionListener(e -> {
            String fullName = fullNameField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String address = addressField.getText().trim();
            if (fullName.isBlank() || username.isBlank() || password.isBlank() || address.isBlank()) {
                app.showError("Registration Error", "All fields are required");
                return;
            }
            registerButton.setEnabled(false);
            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() {
                    return app.getUserService().register(fullName, username, password, address);
                }

                @Override
                protected void done() {
                    registerButton.setEnabled(true);
                    try {
                        get();
                        app.showSuccess("Registration Successful", "User registered successfully");
                        tabs.setSelectedIndex(0);
                    } catch (Exception ex) {
                        app.showError("Registration Error", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    }
                }
            }.execute();
        });

        switchButton.addActionListener(e -> tabs.setSelectedIndex(0));
        return panel;
    }
}
