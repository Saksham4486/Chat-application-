package ChatApplication;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatWindowUI extends JFrame {
    private JPanel chatPanel, userListPanel;
    private JTextField messageField;
    private JScrollPane scrollPane;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private String username;
    public ChatWindowUI(String username) {
        this.username = username;
        setTitle("Chat Application — Group Chat");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 25, 55));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("💬 Group Chat");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel user = new JLabel("Logged in as: " + username);
        user.setForeground(new Color(180, 190, 220));
        user.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        titleBox.add(title);
        titleBox.add(user);

        JButton backBtn = new JButton("Back") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(90, 120, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
            }
        };
        backBtn.setForeground(Color.WHITE);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> {
            dispose();
            new ChatLoginUI();
        });
        header.add(titleBox, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(20, 25, 55));

        JLabel roomLabel = new JLabel("CHAT ROOM");
        roomLabel.setForeground(Color.WHITE);
        roomLabel.setBorder(new EmptyBorder(10, 15, 5, 10));

        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(new Color(20, 25, 55));

        sidebar.add(roomLabel, BorderLayout.NORTH);
        sidebar.add(userListPanel, BorderLayout.CENTER);
        add(sidebar, BorderLayout.WEST);
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(15, 20, 40));
        chatPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(20, 25, 55));
        bottom.setBorder(new EmptyBorder(10, 220, 10, 20));

        messageField = new JTextField();
        messageField.setPreferredSize(new Dimension(0, 40));
        messageField.setBackground(new Color(40, 50, 80));
        messageField.setForeground(Color.WHITE);
        messageField.setCaretColor(Color.WHITE);
        messageField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JButton sendBtn = new JButton("Send") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(90, 120, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
            }
        };
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.setPreferredSize(new Dimension(100, 40));

        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        connectToServer();
        listenForMessages();
        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        setVisible(true);
    }
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect");
        }
    }
    private void listenForMessages() {
        new Thread(() -> {
            String msg;
            try {
                while ((msg = bufferedReader.readLine()) != null) {
                    if (msg.startsWith("USERS:")) {
                        updateUserList(msg.substring(6).split(","));
                    } else {
                        addMessage(msg);
                    }
                }
            } catch (IOException e) {
                addMessage("Server: Disconnected");
            }
        }).start();
    }
    private void updateUserList(String[] users) {
        userListPanel.removeAll();
        for (String user : users) {
            if (!user.isEmpty()) {
                JPanel card = new JPanel();
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setBackground(new Color(255, 255, 255, 20));
                card.setBorder(new EmptyBorder(8, 12, 8, 12));
                JLabel name = new JLabel(user);
                name.setForeground(Color.WHITE);
                card.setLayout(new BorderLayout());
                card.add(name, BorderLayout.WEST);
                userListPanel.add(card);
                userListPanel.add(Box.createVerticalStrut(8));
            }
        }

        userListPanel.revalidate();
        userListPanel.repaint();
    }

    private void sendMessage() {
        try {
            String msg = messageField.getText().trim();
            if (!msg.isEmpty()) {
                bufferedWriter.write(msg);
                bufferedWriter.newLine();
                bufferedWriter.flush();
                messageField.setText("");
            }
        } catch (IOException e) {
            addMessage("Server: Failed");
        }
    }

    private void addMessage(String message) {

        if (message.startsWith("Server:")) {
            JLabel serverMsg = new JLabel(message);
            serverMsg.setForeground(new Color(150, 160, 200));
            serverMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
            chatPanel.add(serverMsg);
            chatPanel.add(Box.createVerticalStrut(4)); // tighter spacing
            return;
        }
        boolean isOwn = message.startsWith(username + ":");
        String sender = message.split(":", 2)[0];
        String content = message.split(":", 2)[1];
        JPanel wrapper = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT));
        wrapper.setOpaque(false);
        JPanel bubbleBox = new JPanel();
        bubbleBox.setLayout(new BoxLayout(bubbleBox, BoxLayout.Y_AXIS));
        bubbleBox.setOpaque(false);
        bubbleBox.setAlignmentX(Component.LEFT_ALIGNMENT); // fix alignment
        if (!isOwn) {
            JLabel name = new JLabel(sender);
            name.setFont(new Font("Segoe UI", Font.BOLD, 11));
            name.setForeground(new Color(180, 190, 220));
            name.setAlignmentX(Component.LEFT_ALIGNMENT); // stays fixed left
            bubbleBox.add(name);
        }
        RoundedBubble bubble = new RoundedBubble(content, isOwn);
        bubbleBox.add(bubble);
        wrapper.add(bubbleBox);
        chatPanel.add(wrapper);
        chatPanel.add(Box.createVerticalStrut(4)); // reduced gap
        chatPanel.revalidate();
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = scrollPane.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }
}

class RoundedBubble extends JPanel {
    public RoundedBubble(String text, boolean isOwn) {
        setLayout(new BorderLayout());
        setOpaque(false);
        JLabel label = new JLabel("<html><div style='max-width:200px;'>" + text + "</div></html>");
        label.setForeground(Color.WHITE);
        label.setBorder(new EmptyBorder(8, 12, 8, 12));
        add(label, BorderLayout.CENTER);
        if (isOwn)
            setBackground(new Color(90, 120, 180));
        else
            setBackground(new Color(40, 50, 80));
    }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        super.paintComponent(g);
    }
}
