import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ExpenseTracker extends JFrame {
    private JTextField dateField, descriptionField, amountField, payeeField, usernameField, passwordField;
    private JComboBox<String> modeCombo;
    private DefaultTableModel tableModel;
    private JTable table;
    private Connection conn;
    private int currentUserId = -1;

    public ExpenseTracker() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql:///expense_tracker", "root", "qwerty");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to database. Please check credentials.");
            return;
        }

        showLoginScreen();
    }

    private void showLoginScreen() {
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(4, 2));

        loginFrame.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginFrame.add(usernameField);

        loginFrame.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginFrame.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> loginUser(loginFrame));
        loginFrame.add(loginButton);

        JButton signupButton = new JButton("Signup");
        signupButton.setBackground(Color.decode("#E74C3C"));
        signupButton.addActionListener(e -> showSignupScreen());
        loginFrame.add(signupButton);

        loginFrame.setVisible(true);
    }

    private void showSignupScreen() {
        JFrame signupFrame = new JFrame("Signup");
        signupFrame.setSize(300, 200);
        signupFrame.setLayout(new GridLayout(5, 2));

        signupFrame.add(new JLabel("Username:"));
        JTextField signupUsername = new JTextField();
        signupFrame.add(signupUsername);

        signupFrame.add(new JLabel("Password:"));
        JPasswordField signupPassword = new JPasswordField();
        signupFrame.add(signupPassword);

        signupFrame.add(new JLabel("Retype Password:"));
        JPasswordField retypePassword = new JPasswordField();
        signupFrame.add(retypePassword);

        JButton signupButton = new JButton("Create Account");
        signupButton.setBackground(Color.decode("#F39C12"));
        signupButton.addActionListener(e -> {
            String username = signupUsername.getText();
            String password = new String(signupPassword.getPassword());
            String retype = new String(retypePassword.getPassword());

            if (password.equals(retype)) {
                if (createUser(username, password)) {
                    JOptionPane.showMessageDialog(signupFrame, "Account created! Please login.");
                    signupFrame.dispose();
                } else {
                    JOptionPane.showMessageDialog(signupFrame, "Username already exists.");
                }
            } else {
                JOptionPane.showMessageDialog(signupFrame, "Passwords do not match.");
            }
        });
        signupFrame.add(signupButton);

        signupFrame.setVisible(true);
    }

    private boolean createUser(String username, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password); // Use hashing for production environments
            stmt.executeUpdate();
            stmt.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void loginUser(JFrame loginFrame) {
        String username = usernameField.getText();
        String password = passwordField.getText(); // Use char[] for password security

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, password); // Use hashing for production environments
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("id");
                loginFrame.dispose();
                showExpenseTracker();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials.");
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showExpenseTracker() {
        setTitle("Expense Tracker");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Data Entry Panel
        JPanel entryPanel = new JPanel(new GridLayout(10, 2, 5, 5));
        entryPanel.add(new JLabel("Date (yyyy-mm-dd):"));
        dateField = new JTextField();
        entryPanel.add(dateField);
        entryPanel.setBackground(Color.decode("#F1C40F"));
        entryPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        entryPanel.add(descriptionField);

        entryPanel.add(new JLabel("Amount:"));
        amountField = new JTextField("0.0");
        entryPanel.add(amountField);

        entryPanel.add(new JLabel("Payee:"));
        payeeField = new JTextField();
        entryPanel.add(payeeField);

        entryPanel.add(new JLabel("Mode of Payment:"));
        modeCombo = new JComboBox<>(new String[]{"Cash", "Credit Card", "UPI", "Google Pay", "Others"});
        entryPanel.add(modeCombo);

        JButton addButton = new JButton("Add Expense");
        addButton.addActionListener(e -> addExpense());
        entryPanel.add(addButton);

        JButton resetButton = new JButton("Reset Fields");
        resetButton.addActionListener(e -> resetFields());
        entryPanel.add(resetButton);

        add(entryPanel, BorderLayout.WEST);

        // Table for displaying expenses
        String[] columns = {"S No.", "Date", "Payee", "Description", "Amount", "Mode of Payment"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Control Panel
        JPanel topPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JButton viewButton = new JButton("View Selected");
        viewButton.addActionListener(e -> viewExpense());
        topPanel.add(viewButton);

        JButton editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> editExpense());
        topPanel.add(editButton);

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteExpense());
        topPanel.add(deleteButton);

        JButton deleteAllButton = new JButton("Delete All");
        deleteAllButton.addActionListener(e -> deleteAllExpenses());
        topPanel.add(deleteAllButton);

        add(topPanel, BorderLayout.NORTH);

        loadExpenses();
    }

    private void addExpense() {
        String date = dateField.getText();
        String description = descriptionField.getText();
        double amount = Double.parseDouble(amountField.getText());
        String payee = payeeField.getText();
        String mode = modeCombo.getSelectedItem().toString();

        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO expenses (user_id, date, payee, description, amount, mode) VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, currentUserId);
            stmt.setString(2, date);
            stmt.setString(3, payee);
            stmt.setString(4, description);
            stmt.setDouble(5, amount);
            stmt.setString(6, mode);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadExpenses();
        resetFields();
    }

    private void loadExpenses() {
        tableModel.setRowCount(0);
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM expenses WHERE user_id = ?");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            int serial = 1;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        serial++, rs.getString("date"), rs.getString("payee"),
                        rs.getString("description"), rs.getDouble("amount"), rs.getString("mode")
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            JOptionPane.showMessageDialog(this, "Details:\nDate: " + tableModel.getValueAt(selectedRow, 1) +
                    "\nPayee: " + tableModel.getValueAt(selectedRow, 2) +
                    "\nDescription: " + tableModel.getValueAt(selectedRow, 3) +
                    "\nAmount: " + tableModel.getValueAt(selectedRow, 4) +
                    "\nMode of Payment: " + tableModel.getValueAt(selectedRow, 5));
        }
    }

    private void editExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to edit.");
            return;
        }

        int id = selectedRow + 1;
        String date = dateField.getText();
        String description = descriptionField.getText();
        double amount = Double.parseDouble(amountField.getText());
        String payee = payeeField.getText();
        String mode = modeCombo.getSelectedItem().toString();

        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE expenses SET date = ?, payee = ?, description = ?, amount = ?, mode = ? WHERE id = ? AND user_id = ?");
            stmt.setString(1, date);
            stmt.setString(2, payee);
            stmt.setString(3, description);
            stmt.setDouble(4, amount);
            stmt.setString(5, mode);
            stmt.setInt(6, id);
            stmt.setInt(7, currentUserId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadExpenses();
    }

    private void deleteExpense() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.");
            return;
        }

        int id = selectedRow + 1;
        try {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM expenses WHERE id = ? AND user_id = ?");
            stmt.setInt(1, id);
            stmt.setInt(2, currentUserId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadExpenses();
    }

    private void deleteAllExpenses() {
        try {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM expenses WHERE user_id = ?");
            stmt.setInt(1, currentUserId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadExpenses();
    }

    private void resetFields() {
        dateField.setText("");
        descriptionField.setText("");
        amountField.setText("0.0");
        payeeField.setText("");
        modeCombo.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExpenseTracker tracker = new ExpenseTracker();
            tracker.setVisible(true);
        });
    }
}
