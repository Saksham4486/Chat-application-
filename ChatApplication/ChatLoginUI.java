package ChatApplication;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class ChatLoginUI extends JFrame {

    public ChatLoginUI() {

        setTitle("Chat Application");
        setSize(600, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 🌌 DARK BACKGROUND
        JPanel background = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(10, 15, 35),
                        0, getHeight(), new Color(20, 25, 55)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new BoxLayout(background, BoxLayout.Y_AXIS));
        background.setBorder(new EmptyBorder(70, 0, 0, 0));

        // 💬 MESSAGE ICON (CUSTOM DRAWN)
        JPanel circle = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;

                // Circle
                g2.setColor(new Color(120, 160, 220));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(5, 5, getWidth()-10, getHeight()-10);

                // Chat bubble inside
                g2.drawRoundRect(28, 30, 34, 22, 8, 8);
                g2.drawLine(35, 52, 32, 60);
                g2.drawLine(32, 60, 42, 52);
            }
        };
        circle.setPreferredSize(new Dimension(90, 90));
        circle.setMaximumSize(new Dimension(90, 90));
        circle.setOpaque(false);
        circle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 🧠 Title
        JLabel title = new JLabel("Welcome to ChatApp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Enter your username to join the conversation");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(170, 180, 200));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 💎 CARD (SMALLER WIDTH)
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(255, 255, 255, 20));
        card.setBorder(new EmptyBorder(25, 25, 25, 25));
        card.setMaximumSize(new Dimension(360, 180)); // reduced width
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Username label (LEFT ALIGNED)
        JLabel userLabel = new JLabel("Username");
        userLabel.setForeground(new Color(200, 210, 230));
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ✏️ Input Field
        JTextField userField = new JTextField("e.g. java_dev_123");
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userField.setBackground(new Color(50, 60, 90));
        userField.setForeground(new Color(170, 170, 170));
        userField.setCaretColor(Color.WHITE);

        userField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 110, 150), 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        userField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Placeholder logic
        userField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (userField.getText().equals("e.g. java_dev_123")) {
                    userField.setText("");
                    userField.setForeground(Color.WHITE);
                }
            }

            public void focusLost(FocusEvent e) {
                if (userField.getText().isEmpty()) {
                    userField.setText("e.g. java_dev_123");
                    userField.setForeground(new Color(170, 170, 170));
                }
            }
        });

        // 🚀 BUTTON (ALIGNED WITH FIELD)
        JButton joinBtn = new JButton("→  Join Chat");
        joinBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        joinBtn.setBackground(new Color(90, 120, 180));
        joinBtn.setForeground(Color.WHITE);
        joinBtn.setFocusPainted(false);
        joinBtn.setBorderPainted(false);
        joinBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        joinBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        joinBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Action
        joinBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            if (username.equals("e.g. java_dev_123") || username.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Enter username!");
                return;
            }
            new ChatWindowUI(username).setVisible(true);
            dispose();
        });

        // Layout inside card
        card.add(userLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(userField);
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(joinBtn);

        // Main layout
        background.add(circle);
        background.add(Box.createRigidArea(new Dimension(0, 20)));
        background.add(title);
        background.add(Box.createRigidArea(new Dimension(0, 10)));
        background.add(subtitle);
        background.add(Box.createRigidArea(new Dimension(0, 40)));
        background.add(card);

        add(background);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatLoginUI::new);
    }
}