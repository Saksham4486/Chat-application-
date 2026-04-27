package ChatApplication;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class ChatLoginUI extends JFrame {

    static final Color BG          = new Color(23, 23, 26);
    static final Color BG_CARD     = new Color(35, 36, 40);
    static final Color BG_INPUT    = new Color(28, 29, 32);
    static final Color BLURPLE     = new Color(88, 101, 242);
    static final Color BLURPLE_DK  = new Color(65,  78, 210);
    static final Color TEXT        = new Color(220, 221, 222);
    static final Color TEXT_MUTED  = new Color(148, 155, 164);
    static final Color SUCCESS     = new Color(35, 165, 90);
    static final Color ERROR       = new Color(237, 66, 69);
    static final Color BORDER      = new Color(255,255,255, 18);

    private JTextField  usernameField;
    private JPasswordField passField;
    private JTextField  serverIpFieldLogin;
    
    private JTextField  displayNameField;
    private JTextField  emailField;
    private JTextField  serverIpFieldReg;

    private JLabel      statusLabel;
    private JPanel      loginCard;
    private JPanel      registerCard;

    public ChatLoginUI() {
        setTitle("ChatApp");
        setSize(460, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        String saved = LocalDB.loadSession();
        if (saved != null && LocalDB.userExists(saved)) {
            LocalDB.UserRecord rec = autoLogin(saved);
            if (rec != null) {
                UserSession.getInstance().set(rec.username, rec.displayName, rec.email);
                
                String savedPath = LocalDB.loadBackupPath(rec.username);
                if (savedPath != null) UserSession.getInstance().setBackupPath(savedPath);
                
                FriendsManager.getInstance().loadFromDB();
                SwingUtilities.invokeLater(() -> { dispose(); new DiscordMainUI(); });
                return;
            }
        }

        buildUI();
        setVisible(true);
    }

    private LocalDB.UserRecord autoLogin(String username) {
        try {
            for (String line : java.nio.file.Files.readAllLines(
                    java.nio.file.Path.of(LocalDB.getDataDir() + "/users.dat"))) {
                String[] p = line.split("\\|", 4);
                if (p[0].equalsIgnoreCase(username)) {
                    return new LocalDB.UserRecord(p[0], p.length>2?p[2]:p[0], p.length>3?p[3]:"");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    
    private void finalizeLogin(String username) {
        String path = LocalDB.loadBackupPath(username);
        if (path == null || path.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Would you like to select a default folder for your chat backups?",
                "Backup Setup", JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select Backup Folder");
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    path = chooser.getSelectedFile().getAbsolutePath();
                    LocalDB.saveBackupPath(username, path);
                }
            }
        }
        
        if (path != null) {
            UserSession.getInstance().setBackupPath(path);
        }

        dispose();
        new DiscordMainUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(30, 0, 16, 0));

        JPanel logo = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLURPLE);
                g2.fillOval(0, 0, 64, 64);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                String initials = "CA";
                g2.drawString(initials, (64 - fm.stringWidth(initials)) / 2, (64 + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(64, 64));
        logo.setMaximumSize(new Dimension(64, 64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appName = new JLabel("ChatApp");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 28));
        appName.setForeground(TEXT);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Talk. Share. Connect.");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tagline.setForeground(TEXT_MUTED);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        top.add(logo);
        top.add(Box.createVerticalStrut(14));
        top.add(appName);
        top.add(Box.createVerticalStrut(4));
        top.add(tagline);

        JPanel cardHolder = new JPanel(new CardLayout());
        cardHolder.setOpaque(false);

        loginCard    = buildLoginCard();
        registerCard = buildRegisterCard();

        cardHolder.add(loginCard,    "login");
        cardHolder.add(registerCard, "register");

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(ERROR);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(0, 20, 10, 20));

        root.add(top,        BorderLayout.NORTH);
        root.add(cardHolder, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
        showLoginCard(cardHolder);
    }

    private JPanel buildLoginCard() {
        JPanel card = roundCard();

        JLabel heading = new JLabel("Sign In");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        usernameField = inputField("Username");
        passField     = passwordField("Password");
        serverIpFieldLogin = inputField("Server IP (e.g. 192.168.1.5 or localhost)");
        
        String savedIp = LocalDB.loadServerIp();
        if (savedIp != null && !savedIp.isEmpty()) {
            serverIpFieldLogin.setText(savedIp);
            serverIpFieldLogin.setForeground(TEXT);
        }

        JButton loginBtn = primaryBtn("Log In");
        loginBtn.addActionListener(e -> doLogin());
        passField.addActionListener(e -> doLogin());
        serverIpFieldLogin.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passField.requestFocus());

        JCheckBox rememberBox = new JCheckBox("Keep me logged in");
        rememberBox.setOpaque(false);
        rememberBox.setForeground(TEXT_MUTED);
        rememberBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberBox.setSelected(true);
        rememberBox.setFocusPainted(false);
        rememberBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        rememberBox.addActionListener(e -> {
            if (!rememberBox.isSelected()) LocalDB.clearSession();
        });

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255,255,255,15));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel noAcct = new JLabel("Don't have an account?");
        noAcct.setForeground(TEXT_MUTED);
        noAcct.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JButton registerLink = linkBtn("Register");
        switchRow.add(noAcct);
        switchRow.add(registerLink);

        registerLink.addActionListener(e -> {
            CardLayout cl = (CardLayout) loginCard.getParent().getLayout();
            cl.show(loginCard.getParent(), "register");
        });

        card.add(heading);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("USERNAME"));
        card.add(Box.createVerticalStrut(4));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("PASSWORD"));
        card.add(Box.createVerticalStrut(4));
        card.add(passField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("SERVER IP"));
        card.add(Box.createVerticalStrut(4));
        card.add(serverIpFieldLogin);
        card.add(Box.createVerticalStrut(10));
        card.add(rememberBox);
        card.add(Box.createVerticalStrut(14));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(14));
        card.add(sep);
        card.add(Box.createVerticalStrut(10));
        card.add(switchRow);

        return card;
    }

    private JPanel buildRegisterCard() {
        JPanel card = roundCard();

        JLabel heading = new JLabel("Create Account");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField regUsername    = inputField("Choose a username");
        JPasswordField regPass    = passwordField("Create a password");
        displayNameField          = inputField("Display name (optional)");
        serverIpFieldReg          = inputField("Server IP (e.g. 192.168.1.5 or localhost)");
        
        String savedIp = LocalDB.loadServerIp();
        if (savedIp != null && !savedIp.isEmpty()) {
            serverIpFieldReg.setText(savedIp);
            serverIpFieldReg.setForeground(TEXT);
        }

        JButton createBtn = primaryBtn("Create Account");
        createBtn.addActionListener(e -> doRegister(regUsername, regPass));

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel haveAcct = new JLabel("Already have an account?");
        haveAcct.setForeground(TEXT_MUTED);
        haveAcct.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JButton loginLink = linkBtn("Sign In");
        switchRow.add(haveAcct);
        switchRow.add(loginLink);

        loginLink.addActionListener(e -> {
            CardLayout cl = (CardLayout) registerCard.getParent().getLayout();
            cl.show(registerCard.getParent(), "login");
        });

        card.add(heading);
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("USERNAME *"));
        card.add(Box.createVerticalStrut(4));
        card.add(regUsername);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("PASSWORD *"));
        card.add(Box.createVerticalStrut(4));
        card.add(regPass);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("DISPLAY NAME"));
        card.add(Box.createVerticalStrut(4));
        card.add(displayNameField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("SERVER IP"));
        card.add(Box.createVerticalStrut(4));
        card.add(serverIpFieldReg);
        card.add(Box.createVerticalStrut(14));
        card.add(createBtn);
        card.add(Box.createVerticalStrut(12));
        card.add(switchRow);

        return card;
    }

    private void doLogin() {
        String user = usernameField.getText().trim();
        String pass = new String(passField.getPassword());
        String ip = serverIpFieldLogin.getText().trim();

        if (user.isEmpty() || user.equals("Username")) { setStatus("Enter your username.", true); return; }
        if (pass.isEmpty()) { setStatus("Enter your password.", true); return; }
        if (ip.isEmpty() || ip.contains("e.g.")) ip = "localhost";

        LocalDB.saveServerIp(ip);

        LocalDB.UserRecord rec = LocalDB.loginUser(user, pass);
        if (rec == null) {
            setStatus("Wrong username or password.", true);
            passField.setText("");
            return;
        }

        UserSession.getInstance().set(rec.username, rec.displayName, rec.email);
        FriendsManager.getInstance().loadFromDB();
        setStatus("Welcome back, " + rec.displayName + "!", false);

        Timer t = new Timer(600, ev -> finalizeLogin(rec.username));
        t.setRepeats(false);
        t.start();
    }

    private void doRegister(JTextField uField, JPasswordField pField) {
        String user    = uField.getText().trim();
        String pass    = new String(pField.getPassword());
        String dname   = displayNameField.getText().trim();
        String ip      = serverIpFieldReg.getText().trim();

        if (user.isEmpty() || user.equals("Choose a username")) { setStatus("Username is required.", true); return; }
        if (user.length()<3)  { setStatus("Username must be at least 3 characters.", true); return; }
        if (pass.isEmpty())   { setStatus("Password is required.", true); return; }
        if (pass.length()<4)  { setStatus("Password must be at least 4 characters.", true); return; }
        if (user.contains("|") || user.contains(" ")) {
            setStatus("Username cannot contain spaces or |", true); return;
        }
        if (ip.isEmpty() || ip.contains("e.g.")) ip = "localhost";

        LocalDB.saveServerIp(ip);

        String displayName = (dname.isEmpty() || dname.equals("Display name (optional)")) ? user : dname;
        boolean ok = LocalDB.registerUser(user, pass, displayName, "");
        if (!ok) { setStatus("Username already taken. Choose another.", true); return; }

        UserSession.getInstance().set(user, displayName, "");
        FriendsManager.getInstance().loadFromDB();
        setStatus("Account created! Welcome, " + displayName + "!", false);

        Timer t = new Timer(600, ev -> finalizeLogin(user));
        t.setRepeats(false);
        t.start();
    }

    private void showLoginCard(JPanel holder) {
        CardLayout cl = (CardLayout) holder.getLayout();
        cl.show(holder, "login");
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setForeground(isError ? ERROR : SUCCESS);
        statusLabel.setText(msg);
    }

    private JPanel roundCard() {
        JPanel card = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 28, 20, 28));
        return card;
    }

    private JTextField inputField(String placeholder) {
        JTextField f = new JTextField() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
            }
        };
        styleField(f, placeholder);
        return f;
    }

    private JPasswordField passwordField(String placeholder) {
        JPasswordField f = new JPasswordField() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
            }
        };
        styleField(f, placeholder);
        return f;
    }

    private void styleField(JTextField f, String placeholder) {
        f.setOpaque(false);
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT);
        f.setCaretColor(BLURPLE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(10, 12, 10, 12)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (f instanceof JPasswordField pf) {
            f.setForeground(TEXT_MUTED);
            f.addFocusListener(new FocusAdapter() {
                boolean first = true;
                public void focusGained(FocusEvent e) {
                    if (first) { pf.setText(""); f.setForeground(TEXT); first=false; }
                }
            });
        } else {
            if (f.getText().isEmpty()) {
                f.setText(placeholder);
                f.setForeground(TEXT_MUTED);
            }
            f.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT); }
                }
                public void focusLost(FocusEvent e) {
                    if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_MUTED); }
                }
            });
        }
    }

    private JButton primaryBtn(String text) {
        JButton btn = new JButton(text) {
            boolean hover = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
                public void mouseExited(MouseEvent e)  { hover=false; repaint(); }
            }); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? BLURPLE_DK : BLURPLE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return btn;
    }

    private JButton linkBtn(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(BLURPLE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("OptionPane.background", new Color(35,36,40));
        UIManager.put("Panel.background", new Color(35,36,40));
        SwingUtilities.invokeLater(ChatLoginUI::new);
    }
}