import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class OnlineBankingApp {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea transactionHistoryArea;
    private JTextField transferAmountField;
    private JTextField transferToField;
    private Connection connection;
    private int currentUserId; 

    public OnlineBankingApp() {
        initialize();
        connectToDatabase();
    }

    private void initialize() {
        frame = new JFrame("Online Banking System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());

        JPanel loginPanel = new JPanel(new GridLayout(3, 2));
        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new LoginAction());
        loginPanel.add(loginButton);

        frame.add(loginPanel, BorderLayout.NORTH);

        transactionHistoryArea = new JTextArea();
        transactionHistoryArea.setEditable(false);
        frame.add(new JScrollPane(transactionHistoryArea), BorderLayout.CENTER);

        JPanel transferPanel = new JPanel(new GridLayout(3, 2));
        transferPanel.add(new JLabel("Transfer To (User ID):"));
        transferToField = new JTextField();
        transferPanel.add(transferToField);
        transferPanel.add(new JLabel("Amount:"));
        transferAmountField = new JTextField();
        transferPanel.add(transferAmountField);
        JButton transferButton = new JButton("Transfer");
        transferButton.addActionListener(new TransferAction());
        transferPanel.add(transferButton);

        frame.add(transferPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/OnlineBanking", "root", "aniket3404");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTransactionHistory() {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Transactions WHERE user_id = ?");
            stmt.setInt(1, currentUserId);
            ResultSet rs = stmt.executeQuery();
            transactionHistoryArea.setText("");
            while (rs.next()) {
                transactionHistoryArea.append("Transaction ID: " + rs.getInt("id") + ", Amount: " + rs.getDouble("amount") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Users WHERE username = ? AND password = ?");
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    updateTransactionHistory();
                } else {
                    JOptionPane.showMessageDialog(frame, "Invalid credentials");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class TransferAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int transferToId = Integer.parseInt(transferToField.getText());
                double amount = Double.parseDouble(transferAmountField.getText());

                PreparedStatement checkBalanceStmt = connection.prepareStatement("SELECT balance FROM Users WHERE id = ?");
                checkBalanceStmt.setInt(1, currentUserId);
                ResultSet balanceResult = checkBalanceStmt.executeQuery();
                if (balanceResult.next()) {
                    double currentBalance = balanceResult.getDouble("balance");
                    if (currentBalance >= amount) {
                        
                        PreparedStatement deductStmt = connection.prepareStatement("UPDATE Users SET balance = balance - ? WHERE id = ?");
                        deductStmt.setDouble(1, amount);
                        deductStmt.setInt(2, currentUserId);
                        deductStmt.executeUpdate();

                      
                        PreparedStatement addStmt = connection.prepareStatement("UPDATE Users SET balance = balance + ? WHERE id = ?");
                        addStmt.setDouble(1, amount);
                        addStmt.setInt(2, transferToId);
                        addStmt.executeUpdate();

                        
                        PreparedStatement recordTransaction = connection.prepareStatement("INSERT INTO Transactions (user_id, amount) VALUES (?, ?)");
                        recordTransaction.setInt(1, currentUserId);
                        recordTransaction.setDouble(2, -amount);
                        recordTransaction.executeUpdate();

                        JOptionPane.showMessageDialog(frame, "Transfer successful!");
                        updateTransactionHistory();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Insufficient balance.");
                    }
                }
            } catch (SQLException | NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new OnlineBankingApp();
    }
}
