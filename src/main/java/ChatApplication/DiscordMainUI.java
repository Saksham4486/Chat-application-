package ChatApplication;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class DiscordMainUI extends JFrame {

    static final Color BG_DARKEST  = new Color(17,  18,  20);
    static final Color BG_SIDEBAR  = new Color(30,  31,  34);
    static final Color BG_CHANNELS = new Color(43,  45,  49);
    static final Color BG_CHAT     = new Color(54,  57,  62);
    static final Color BG_INPUT    = new Color(64,  68,  75);
    static final Color BG_HOVER    = new Color(65,  68,  74);
    static final Color BG_SEL      = new Color(88,  91,  98);
    static final Color BLURPLE     = new Color(88, 101, 242);
    static final Color BLURPLE_DK  = new Color(65,  78, 210);
    static final Color TEXT        = new Color(220, 221, 222);
    static final Color TEXT_MUTED  = new Color(148, 155, 164);
    static final Color TEXT_HDR    = new Color(242, 243, 245);
    static final Color GREEN       = new Color(35, 165, 90);
    static final Color RED         = new Color(237, 66, 69);
    static final Color DIVIDER     = new Color(30,  31,  34);
    static final Color MENTION_BG  = new Color(88, 101, 242, 25);

    private final UserSession   session = UserSession.getInstance();
    private final FriendsManager fm     = FriendsManager.getInstance();

    private JPanel      chatPanel;
    private JScrollPane chatScroll;
    private JTextField  chatInput;
    private JLabel      channelTitle, channelDesc;

    // Reply Feature UI
    private JPanel replyPanel;
    private JLabel replyTextLabel;
    private String replyingToMsg = null;
    
    // Profile Picture Support
    private Image userProfileImage = null;

    private JPanel friendsListPanel;
    private JPanel serverRail;

    private BufferedWriter netOut;
    private BufferedReader netIn;
    private Socket         netSocket;

    private String activeChannel = "general";
    private String activeDMUser  = null;

    public DiscordMainUI() {
        setTitle("ChatApp — " + session.getDisplayName());
        setSize(1200, 760);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        getContentPane().setBackground(BG_DARKEST);
        setLayout(new BorderLayout());

        add(buildTitleBar(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);

        serverRail = buildServerRail();
        main.add(serverRail, BorderLayout.WEST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildMiddlePanel(), buildChatArea());
        split.setDividerSize(0);
        split.setDividerLocation(240);
        split.setBorder(null);
        split.setOpaque(false);
        split.setContinuousLayout(true);
        main.add(split, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        // Load avatar if previously saved
        loadSavedAvatar();

        connectToServer();
        listenForMessages();
        showFriendsView();
        setVisible(true);
    }
    
    private void loadSavedAvatar() {
        String avatarPath = LocalDB.getDataDir() + "/avatar_" + session.getUsername() + ".png";
        File f = new File(avatarPath);
        if (f.exists()) {
            try {
                userProfileImage = ImageIO.read(f);
            } catch (IOException ignored) {}
        }
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_DARKEST);
        bar.setPreferredSize(new Dimension(0, 38));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));

        JLabel lbl = new JLabel("  💬 ChatApp");
        lbl.setForeground(TEXT);
        lbl.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        bar.add(lbl, BorderLayout.WEST);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        ctrl.setOpaque(false);
        JButton min = ctrlBtn("—", new Color(80,80,80));
        min.addActionListener(e -> setState(Frame.ICONIFIED));
        JButton close = ctrlBtn("✕", new Color(190,60,60));
        close.addActionListener(e -> System.exit(0));
        ctrl.add(min); ctrl.add(close);
        bar.add(ctrl, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildServerRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(BG_DARKEST);
        rail.setPreferredSize(new Dimension(72, 0));
        rail.setBorder(new EmptyBorder(8, 0, 8, 0));
        JPanel home = railIcon("🏠", "Home (Friends & DMs)", BLURPLE);
        home.addMouseListener(click(() -> showFriendsView()));
        rail.add(home);
        rail.add(railDiv());
        JPanel gen = railIcon("G", "General Chat", new Color(60, 80, 140));
        gen.addMouseListener(click(() -> openChannel("general")));
        rail.add(gen);
        rail.add(railDiv());
        refreshGroupIcons(rail);
        rail.add(addGroupBtn(rail));
        return rail;
    }

    private void refreshGroupIcons(JPanel rail) {}

    private JPanel buildGroupIcon(String name, JPanel rail) {
        String label = name.length() >= 2 ? name.substring(0,2).toUpperCase() : name.toUpperCase();
        Color color = new Color(Math.abs(name.hashCode()) % 180 + 60, 90, 180);
        JPanel icon = railIcon(label, name, color);
        icon.addMouseListener(click(() -> openGroup(name)));
        return icon;
    }

    private JPanel addGroupBtn(JPanel rail) {
        JPanel p = new JPanel() {
            boolean hover = false;
            { setCursor(new Cursor(Cursor.HAND_CURSOR));
              setToolTipText("Create or Join Group");
              addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
                public void mouseExited(MouseEvent e)  { hover=false; repaint(); }
                public void mouseClicked(MouseEvent e) { showCreateGroupDialog(rail); }
              });
            }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int m = hover ? 6 : 12; int sz = getWidth()-m*2;
                g2.setColor(hover ? GREEN : BG_CHANNELS);
                if (hover) g2.fillRoundRect(m,(getHeight()-sz)/2,sz,sz,16,16);
                else       g2.fillOval(m,(getHeight()-sz)/2,sz,sz);
                g2.setColor(hover ? Color.WHITE : GREEN);
                g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,22));
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString("+",(getWidth()-fm2.stringWidth("+"))/2,
                    (getHeight()+fm2.getAscent()-fm2.getDescent())/2);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(72, 52));
        p.setMaximumSize(new Dimension(72, 52));
        return p;
    }

    private JPanel buildMiddlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CHANNELS);
        panel.setPreferredSize(new Dimension(240, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(38, 40, 44));
        header.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,DIVIDER),
            new EmptyBorder(12,14,12,14)));
        header.setPreferredSize(new Dimension(0,50));
        JLabel title = new JLabel("💬 ChatApp");
        title.setFont(new Font("Segoe UI Symbol", Font.BOLD, 15));
        title.setForeground(TEXT_HDR);
        header.add(title, BorderLayout.WEST);
        panel.add(header, BorderLayout.NORTH);

        friendsListPanel = new JPanel();
        friendsListPanel.setLayout(new BoxLayout(friendsListPanel, BoxLayout.Y_AXIS));
        friendsListPanel.setBackground(BG_CHANNELS);

        JScrollPane scroll = new JScrollPane(friendsListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CHANNELS);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        panel.add(scroll, BorderLayout.CENTER);

        panel.add(buildUserCard(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildUserCard() {
        JPanel card = new JPanel(new BorderLayout(8,0));
        card.setBackground(new Color(35,36,40));
        card.setBorder(new EmptyBorder(8,10,8,10));
        card.setPreferredSize(new Dimension(0,54));

        JPanel av = avatar(session.getInitials(), session.getAvatarColor(), 36, userProfileImage);
        card.add(av, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel name = new JLabel(session.getDisplayName());
        name.setFont(new Font("Segoe UI Symbol", Font.BOLD, 13));
        name.setForeground(TEXT);
        JLabel status = new JLabel("● Online");
        status.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 11));
        status.setForeground(GREEN);
        info.add(name); info.add(status);
        card.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        btns.setOpaque(false);
        JButton settingsBtn = iconBtn("⚙");
        settingsBtn.setToolTipText("Settings");
        settingsBtn.addActionListener(e -> showSettings());
        btns.add(settingsBtn);
        card.add(btns, BorderLayout.EAST);
        return card;
    }

    private JPanel buildChatArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(BG_CHAT);

        JPanel topBar = new JPanel(new BorderLayout(10,0));
        topBar.setBackground(BG_CHAT);
        topBar.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,DIVIDER),
            new EmptyBorder(10,16,10,16)));
        topBar.setPreferredSize(new Dimension(0,52));

        channelTitle = new JLabel("# general");
        channelTitle.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        channelTitle.setForeground(TEXT_HDR);

        channelDesc = new JLabel("General chat room");
        channelDesc.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12));
        channelDesc.setForeground(TEXT_MUTED);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(channelTitle);
        titleBox.add(channelDesc);
        topBar.add(titleBox, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton addFriend = smallBtn("+ Add Friend", BLURPLE);
        addFriend.addActionListener(e -> showAddFriendDialog());
        JButton backup = smallBtn("☁ Backup", new Color(50,110,90));
        backup.addActionListener(e -> performBackup());
        actions.add(addFriend);
        actions.add(backup);
        topBar.add(actions, BorderLayout.EAST);
        area.add(topBar, BorderLayout.NORTH);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BG_CHAT);
        chatPanel.setBorder(new EmptyBorder(12, 8, 8, 8));

        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(BG_CHAT);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        area.add(chatScroll, BorderLayout.CENTER);

        area.add(buildInputBar(), BorderLayout.SOUTH);
        return area;
    }

    private JPanel buildInputBar() {
    JPanel outer = new JPanel(new BorderLayout(10, 0));
    outer.setBackground(BG_CHAT);
    outer.setBorder(new EmptyBorder(0, 16, 16, 16));

    // ✅ Emoji-friendly font for text input, but we will draw icons manually
    Font emojiFont = new Font("Segoe UI Emoji", Font.PLAIN, 18);

    // ================= REPLY PANEL =================
    replyPanel = new JPanel(new BorderLayout());
    replyPanel.setBackground(new Color(43, 45, 49));
    replyPanel.setBorder(new CompoundBorder(
        new MatteBorder(0, 4, 0, 0, BLURPLE),
        new EmptyBorder(8, 12, 8, 12)
    ));
    replyPanel.setVisible(false);

    replyTextLabel = new JLabel("Replying to...");
    replyTextLabel.setFont(emojiFont.deriveFont(Font.ITALIC, 13f));
    replyTextLabel.setForeground(TEXT_MUTED);

    // Custom close button for reply panel
    JButton cancelReplyBtn = new JButton("✕") {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, 
                         (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }
    };
    cancelReplyBtn.setContentAreaFilled(false);
    cancelReplyBtn.setBorderPainted(false);
    cancelReplyBtn.setFocusPainted(false);
    cancelReplyBtn.setPreferredSize(new Dimension(28, 28));
    cancelReplyBtn.addActionListener(e -> {
        replyingToMsg = null;
        replyPanel.setVisible(false);
    });

    replyPanel.add(replyTextLabel, BorderLayout.CENTER);
    replyPanel.add(cancelReplyBtn, BorderLayout.EAST);

    // ================= INPUT BOX =================
    JPanel box = new JPanel(new BorderLayout(10, 0)) {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG_INPUT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        }
    };
    box.setOpaque(false);
    box.setPreferredSize(new Dimension(0, 52));

    Dimension btnSize = new Dimension(42, 42);

    // ✅ CUSTOM ATTACHMENT BUTTON (Replaces 📎)
    JButton attachBtn = new JButton() {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT_MUTED);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(11, 11, 20, 20); // Circle
            g2.drawLine(21, 16, 21, 26); // Vertical line
            g2.drawLine(16, 21, 26, 21); // Horizontal line
        }
    };
    attachBtn.setPreferredSize(btnSize);
    attachBtn.setContentAreaFilled(false);
    attachBtn.setBorderPainted(false);
    attachBtn.setFocusPainted(false);
    attachBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    attachBtn.setToolTipText("Send file");
    attachBtn.addActionListener(e -> sendFile());

    // Text Input
    chatInput = new JTextField();
    chatInput.setOpaque(false);
    chatInput.setBackground(new Color(0, 0, 0, 0));
    chatInput.setForeground(TEXT);
    chatInput.setCaretColor(BLURPLE);
    chatInput.setFont(emojiFont);
    chatInput.setBorder(new EmptyBorder(14, 8, 14, 8));
    chatInput.addActionListener(e -> sendMessage());

    // Emoji Picker Trigger (Simple text fallback to avoid boxes)
    JButton emojiBtn = inputIconBtn("☺"); 
    emojiBtn.setPreferredSize(btnSize);
    emojiBtn.addActionListener(e -> showEmojiPicker());

    // ✅ CUSTOM SEND BUTTON (Replaces ➤)
    JButton sendBtn = new JButton() {
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BLURPLE);
            int[] xPoints = {14, 34, 14, 20}; // Triangle coordinates for plane
            int[] yPoints = {14, 21, 28, 21};
            g2.fillPolygon(xPoints, yPoints, 4);
        }
    };
    sendBtn.setPreferredSize(btnSize);
    sendBtn.setContentAreaFilled(false);
    sendBtn.setBorderPainted(false);
    sendBtn.setFocusPainted(false);
    sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    sendBtn.addActionListener(e -> sendMessage());

    // Assemble Layout
    box.add(attachBtn, BorderLayout.WEST);
    box.add(chatInput, BorderLayout.CENTER);
    box.add(emojiBtn, BorderLayout.EAST);

    JPanel centerWrapper = new JPanel(new BorderLayout(0, 6));
    centerWrapper.setOpaque(false);
    centerWrapper.add(replyPanel, BorderLayout.NORTH);
    centerWrapper.add(box, BorderLayout.CENTER);

    outer.add(centerWrapper, BorderLayout.CENTER);
    outer.add(sendBtn, BorderLayout.EAST);

    return outer;
}

    private void showFriendsView() {
        activeDMUser  = null;
        activeChannel = "general";
        channelTitle.setText("Friends");
        channelDesc.setText("Your friends list");
        chatInput.setToolTipText("Message # general");

        friendsListPanel.removeAll();

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        addRow.setOpaque(false);
        JButton addBtn = smallBtn("+ Add Friend", BLURPLE);
        addBtn.addActionListener(e -> showAddFriendDialog());
        addRow.add(addBtn);
        friendsListPanel.add(addRow);

        Map<String, Boolean> friends = fm.getFriends();
        long onlineCount = friends.values().stream().filter(v->v).count();
        friendsListPanel.add(sectionLabel("ONLINE — " + onlineCount));

        boolean anyOnline = false;
        for (Map.Entry<String,Boolean> e : friends.entrySet()) {
            if (e.getValue()) { friendsListPanel.add(friendRow(e.getKey(), true)); anyOnline=true; }
        }
        if (!anyOnline) friendsListPanel.add(mutedNote("No friends online right now."));

        long offlineCount = friends.size() - onlineCount;
        if (offlineCount > 0) {
            friendsListPanel.add(sectionLabel("OFFLINE — " + offlineCount));
            for (Map.Entry<String,Boolean> e : friends.entrySet()) {
                if (!e.getValue()) friendsListPanel.add(friendRow(e.getKey(), false));
            }
        }
        if (friends.isEmpty()) {
            friendsListPanel.add(sectionLabel("ALL FRIENDS"));
            friendsListPanel.add(mutedNote("No friends yet — add someone!"));
        }

        List<String> pending = fm.getPendingRequests();
        if (!pending.isEmpty()) {
            friendsListPanel.add(sectionLabel("PENDING — " + pending.size()));
            for (String p : pending) friendsListPanel.add(pendingRow(p));
        }

        Map<String, List<String>> groups = fm.getGroups();
        if (!groups.isEmpty()) {
            friendsListPanel.add(sectionLabel("YOUR GROUPS"));
            for (String grpName : groups.keySet()) {
                friendsListPanel.add(groupRow(grpName));
            }
        }

        friendsListPanel.add(sectionLabel("DIRECT MESSAGES"));
        for (String f : friends.keySet()) friendsListPanel.add(dmRow(f));
        if (friends.isEmpty()) friendsListPanel.add(mutedNote("Add friends to start DMs."));

        friendsListPanel.revalidate();
        friendsListPanel.repaint();
    }

    private void openChannel(String channel) {
        activeDMUser  = null;
        activeChannel = channel;
        channelTitle.setText("# " + channel);
        channelDesc.setText("General chat room");
        chatInput.setToolTipText("Message #" + channel);
        loadHistory(channel);
        sendSystemMsg("Joined # " + channel);
    }

    private void openDM(String targetUser) {
        activeDMUser  = targetUser;
        activeChannel = "@" + targetUser;
        channelTitle.setText("@ " + targetUser);
        channelDesc.setText("Direct Message");
        chatInput.setToolTipText("Message " + targetUser);
        loadHistory("dm_" + dmKey(session.getUsername(), targetUser));
        sendSystemMsg("Started DM with " + targetUser);
    }

    private void openGroup(String groupName) {
        activeDMUser  = null;
        activeChannel = groupName;
        channelTitle.setText("# " + groupName);
        List<String> members = fm.getGroupMembers(groupName);
        channelDesc.setText(members.size() + " members");
        chatInput.setToolTipText("Message # " + groupName);

        friendsListPanel.removeAll();
        JPanel invRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        invRow.setOpaque(false);
        JButton invBtn = smallBtn("📨 Invite", BLURPLE);
        invBtn.addActionListener(e -> showInviteDialog(groupName));
        invRow.add(invBtn);
        friendsListPanel.add(invRow);
        friendsListPanel.add(sectionLabel("MEMBERS — " + members.size()));
        for (String m : members) friendsListPanel.add(memberRow(m));
        friendsListPanel.revalidate();
        friendsListPanel.repaint();

        loadHistory(groupName);
        sendRaw("/joingroup " + groupName);
    }

    private void loadHistory(String channel) {
        chatPanel.removeAll();
        List<LocalDB.StoredMessage> history = LocalDB.getMessages(channel, 100);
        if (history.isEmpty()) {
            sendSystemMsg("This is the beginning of # " + activeChannel);
        } else {
            sendSystemMsg("─── Chat history ───");
            for (LocalDB.StoredMessage msg : history) {
                boolean own = msg.sender.equalsIgnoreCase(session.getDisplayName());
                appendBubble(msg.sender, msg.content, msg.timestamp, own, false);
            }
        }
    }

    private String dmKey(String a, String b) {
        String[] arr = {a.toLowerCase(), b.toLowerCase()};
        Arrays.sort(arr);
        return arr[0] + "_" + arr[1];
    }

    private void showCreateGroupDialog(JPanel rail) {
        JDialog dlg = dialog("Create a Group", 400, 420); // Increased height for checkboxes
        JPanel c = dialogContent();

        header(c, "Create Group", "Give your group a name and add friends.", dlg);
        c.add(Box.createVerticalStrut(14));
        c.add(dlgLabel("GROUP NAME"));
        c.add(Box.createVerticalStrut(5));
        JTextField nameField = dlgField("e.g. Study Buddies");
        c.add(nameField);
        c.add(Box.createVerticalStrut(14));

        // Friend Selection Checkboxes
        c.add(dlgLabel("SELECT MEMBERS"));
        c.add(Box.createVerticalStrut(5));
        
        JPanel friendsPanel = new JPanel();
        friendsPanel.setLayout(new BoxLayout(friendsPanel, BoxLayout.Y_AXIS));
        friendsPanel.setBackground(new Color(28, 29, 32));
        
        List<JCheckBox> checkboxes = new ArrayList<>();
        for (String friend : fm.getFriends().keySet()) {
            JCheckBox cb = new JCheckBox(" " + friend);
            cb.setOpaque(false);
            cb.setForeground(TEXT);
            cb.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
            cb.setFocusPainted(false);
            cb.setBorder(new EmptyBorder(4, 4, 4, 4));
            checkboxes.add(cb);
            friendsPanel.add(cb);
        }
        
        JScrollPane scroll = new JScrollPane(friendsPanel);
        scroll.setPreferredSize(new Dimension(300, 120));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,18), 1));
        c.add(scroll);
        c.add(Box.createVerticalStrut(14));

        JButton createBtn = primaryBtn2("Create Group");
        c.add(createBtn);
        c.add(Box.createVerticalStrut(8));
        JButton cancel = ghostBtn2("Cancel");
        cancel.addActionListener(e -> dlg.dispose());
        c.add(cancel);

        createBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty() || name.equals("e.g. Study Buddies")) return;
            
            fm.createGroup(name, session.getDisplayName());
            sendRaw("/creategroup " + name);
            
            // Send invites to selected checkboxes
            for (JCheckBox cb : checkboxes) {
                if (cb.isSelected()) {
                    sendRaw("/invite " + name + " " + cb.getText().trim());
                }
            }

            dlg.dispose();
            JPanel icon = buildGroupIcon(name, rail);
            rail.add(icon, rail.getComponentCount() - 1);
            rail.revalidate(); rail.repaint();
            showFriendsView();
            openGroup(name);
            appendSystemMsg("🎉 Group '" + name + "' created!");
        });

        dlg.setContentPane(c);
        dlg.setVisible(true);
    }

    private void showAddFriendDialog() {
        JDialog dlg = dialog("Add Friend", 400, 260);
        JPanel c = dialogContent();

        header(c, "Add a Friend", "You can add friends with their username.", dlg);
        c.add(Box.createVerticalStrut(14));
        JTextField userField = dlgField("Enter username");
        c.add(userField);
        c.add(Box.createVerticalStrut(10));

        JLabel resultLbl = new JLabel(" ");
        resultLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 12));
        resultLbl.setForeground(GREEN); 
        c.add(resultLbl);
        c.add(Box.createVerticalStrut(8));

        JButton sendBtn = primaryBtn2("Send Friend Request");
        c.add(sendBtn);

        sendBtn.addActionListener(e -> {
            String target = userField.getText().trim();
            if (target.isEmpty() || target.equals("Enter username")) return;
            
            if (target.equalsIgnoreCase(session.getUsername())) {
                resultLbl.setForeground(RED);
                resultLbl.setText("You can't add yourself!");
                return;
            }
            
            boolean isOnline = fm.getOnlineUsers().stream().anyMatch(u -> u.equalsIgnoreCase(target));
            
            if (!isOnline) {
                resultLbl.setForeground(RED);
                resultLbl.setText("User '" + target + "' is not online right now.");
                return;
            }
            
            sendRaw("/friend " + target);
            
            resultLbl.setForeground(GREEN);
            resultLbl.setText("✅ Friend request sent to " + target + "!");
            showFriendsView();
        
            javax.swing.Timer t = new javax.swing.Timer(1500, ev -> dlg.dispose());
            t.setRepeats(false);
            t.start();
        });

        dlg.setContentPane(c);
        dlg.setVisible(true);
    }

    private void showInviteDialog(String groupName) {
        JDialog dlg = dialog("Invite to " + groupName, 400, 230);
        JPanel c = dialogContent();
        header(c, "Invite to " + groupName, "Enter the username to invite.", dlg);
        c.add(Box.createVerticalStrut(14));
        JTextField f = dlgField("Username");
        c.add(f);
        c.add(Box.createVerticalStrut(12));
        JButton invBtn = primaryBtn2("Send Invite");
        c.add(invBtn);
        invBtn.addActionListener(e -> {
            String user = f.getText().trim();
            if (!user.isEmpty()) {
                sendRaw("/invite " + groupName + " " + user);
                appendSystemMsg("📨 Invite sent to " + user);
                dlg.dispose();
            }
        });
        dlg.setContentPane(c);
        dlg.setVisible(true);
    }

    private void showEmojiPicker() {
        String[] emojis = {"😊","😂","❤️","👍","🔥","🎉","😎","🤔","👋","🙏",
                           "😍","💯","🚀","⭐","🎮","🎵","😅","🥳","💬","📎"};
        JDialog dlg = dialog("Emoji", 300, 130);
        JPanel grid = new JPanel(new GridLayout(2, 10, 2, 2));
        grid.setBackground(BG_CHANNELS);
        grid.setBorder(new EmptyBorder(10,10,10,10));
        for (String em : emojis) {
            JButton b = new JButton(em);
            b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> { chatInput.setText(chatInput.getText()+em); chatInput.requestFocus(); dlg.dispose(); });
            grid.add(b);
        }
        dlg.setContentPane(grid);
        dlg.setVisible(true);
    }

    private void showSettings() {
        JDialog dlg = dialog("Settings", 420, 360); // Increased height
        JPanel c = dialogContent();

        header(c, "User Settings", "", dlg);
        c.add(Box.createVerticalStrut(14));

        JLabel[] info = {
            new JLabel("Username:   " + session.getUsername()),
            new JLabel("Display Name: " + session.getDisplayName()),
            new JLabel("Email:      " + (session.getEmail().isEmpty() ? "(not set)" : session.getEmail())),
            new JLabel("Data folder:  " + LocalDB.getDataDir()),
        };
        for (JLabel l : info) {
            l.setForeground(TEXT_MUTED);
            l.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.add(l);
            c.add(Box.createVerticalStrut(6));
        }

        c.add(Box.createVerticalStrut(10));
        
        // Profile Picture Button
        JButton avatarBtn = ghostBtn2("📷 Set Profile Picture");
        avatarBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "jpeg"));
            if (chooser.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try {
                    File selected = chooser.getSelectedFile();
                    BufferedImage img = ImageIO.read(selected);
                    
                    // Save locally
                    String savePath = LocalDB.getDataDir() + "/avatar_" + session.getUsername() + ".png";
                    ImageIO.write(img, "png", new File(savePath));
                    
                    userProfileImage = img;
                    JOptionPane.showMessageDialog(dlg, "Avatar updated! It will appear on your messages.");
                    // Force refresh bottom card
                    buildMiddlePanel();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Error saving avatar.");
                }
            }
        });
        c.add(avatarBtn);
        c.add(Box.createVerticalStrut(14));

        JButton logoutBtn = new JButton("Log Out") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RED);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        logoutBtn.setContentAreaFilled(false); logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false); logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logoutBtn.addActionListener(e -> {
            session.logout();
            dlg.dispose(); dispose();
            new ChatLoginUI();
        });
        c.add(logoutBtn);

        dlg.setContentPane(c);
        dlg.setVisible(true);
    }

    private void connectToServer() {
        String serverIp = LocalDB.loadServerIp();
        if (serverIp == null || serverIp.isEmpty()) serverIp = "localhost";

        boolean connected = attemptConnection(serverIp);

        if (!connected) {
            String newIp = JOptionPane.showInputDialog(this,
                    "Could not find server at '" + serverIp + "'.\n" +
                    "Enter the Server's LAN IP Address (e.g. 192.168.1.5)\n" +
                    "Leave as 'localhost' if running on the same laptop:",
                    serverIp);

            if (newIp != null) {
                if (newIp.trim().isEmpty()) newIp = "localhost";
                
                if (attemptConnection(newIp.trim())) {
                    LocalDB.saveServerIp(newIp.trim()); 
                    appendSystemMsg("✅ Connected to server at " + newIp.trim());
                } else {
                    appendSystemMsg("⚠ Could not connect to " + newIp + ". Messages won't be delivered.");
                }
            } else {
                appendSystemMsg("⚠ Offline mode. Messages won't be delivered.");
            }
        } else {
            if (!serverIp.equals("localhost")) {
                appendSystemMsg("✅ Connected to server at " + serverIp);
            }
        }
    }

    private boolean attemptConnection(String ip) {
        try {
            netSocket = new Socket(ip, 1234);
            netOut = new BufferedWriter(new OutputStreamWriter(netSocket.getOutputStream()));
            netIn  = new BufferedReader(new InputStreamReader(netSocket.getInputStream()));
            
            netOut.write(session.getDisplayName());
            netOut.newLine();
            netOut.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                String line;
                while ((line = netIn.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> {
                        if (msg.startsWith("USERS:")) {
                            fm.setOnlineUsers(msg.substring(6).split(","));
                            showFriendsView();
                        } else if (msg.startsWith("FILE|")) {
                            String[] p = FileData.parseFileMessage(msg);
                            if (p != null) appendFileMsg(p);
                        } else if (msg.startsWith("FRIEND_REQUEST:")) {
                            String from = msg.substring(15);
                            fm.addPendingRequest(from);
                            showFriendsView();
                            appendSystemMsg("👋 Friend request from " + from + "!");
                        } else if (msg.startsWith("FRIEND_ACCEPT:")) {
                            String from = msg.substring(14);
                            fm.addFriend(from);
                            showFriendsView();
                            appendSystemMsg("✅ " + from + " accepted your friend request!");
                        } else {
                            handleIncomingText(msg);
                        }
                    });
                }
            } catch (IOException ignored) {}
        }, "netListener").start();
    }

    private void handleIncomingText(String raw) {
        if (raw.startsWith("Server:")) { appendSystemMsg(raw.substring(7).trim()); return; }
        if (!raw.contains(": ")) { appendSystemMsg(raw); return; }
        String[] parts = raw.split(": ", 2);
        String sender  = parts[0].replaceAll("[\\[\\(].*?[\\]\\)]\\s*", "").trim();
        String content = parts[1];
        boolean isOwn  = sender.equalsIgnoreCase(session.getDisplayName());
        boolean special= raw.startsWith("(Private") || raw.startsWith("[");

        if (isOwn) return; // Prevent local echo from server

        appendBubble(sender, content, now(), isOwn, special);
        LocalDB.saveMessage(activeChannel, sender, content);
    }

    private void sendMessage() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        
        // Wrap with reply context if active
        if (replyingToMsg != null) {
            text = "┌ Replying to: " + replyingToMsg + "\n" + text;
            replyingToMsg = null;
            replyPanel.setVisible(false);
        }

        String channel;
        if (activeDMUser != null) {
            sendRaw("/msg " + activeDMUser + " " + text.replace("\n", " // "));
            channel = "dm_" + dmKey(session.getUsername(), activeDMUser);
        } else if (activeChannel != null && !activeChannel.equals("general")) {
            sendRaw("/g " + activeChannel + " " + text.replace("\n", " // "));
            channel = activeChannel;
        } else {
            sendRaw(text.replace("\n", " // "));
            channel = "general";
        }

        appendBubble(session.getDisplayName(), text, now(), true, activeDMUser != null);
        LocalDB.saveMessage(channel, session.getDisplayName(), text.replace("\n", " // "));
        chatInput.setText("");
    }

    private void sendFile() {
        new Thread(() -> {
            String fileMsg = FileShareManager.chooseAndEncodeFile(this, session.getDisplayName(), null);
            if (fileMsg == null) return;
            String toSend;
            if (activeDMUser != null)         toSend = "/fileto " + activeDMUser + " " + fileMsg;
            else if (!activeChannel.equals("general")) toSend = "/gfile " + activeChannel + " " + fileMsg;
            else                              toSend = fileMsg;
            sendRaw(toSend);
            LocalDB.saveMessage(activeChannel, session.getDisplayName(), "[File]");
        }).start();
    }

    private void sendRaw(String raw) {
        if (netOut == null) return;
        try { netOut.write(raw); netOut.newLine(); netOut.flush(); }
        catch (IOException e) { appendSystemMsg("⚠ Send failed."); }
    }

    private void performBackup() {
    try {
        // 1. Force a manual size that we know works (400 width, 450 height)
        JDialog dlg = new JDialog(this, "Backup Configuration", true);
        dlg.setUndecorated(true);
        dlg.setLayout(new BorderLayout());
        dlg.setSize(400, 450); 
        dlg.setLocationRelativeTo(this);

        // 2. Use your existing dialogContent helper
        JPanel c = dialogContent(); 
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        
        // 3. Add the Header with the close button
        header(c, "Backup Chat", "Choose your storage preference.", dlg);
        c.add(Box.createVerticalStrut(20));

        // 4. Email Section
        JLabel emailLabel = dlgLabel("GOOGLE ACCOUNT EMAIL");
        c.add(emailLabel);
        c.add(Box.createVerticalStrut(8));
        
        JTextField emailField = dlgField(session.getEmail().isEmpty() ? "example@gmail.com" : session.getEmail());
        c.add(emailField);
        c.add(Box.createVerticalStrut(25));

        // 5. Buttons
        JButton localOnlyBtn = primaryBtn2("💾 Save JSON Locally Only");
        localOnlyBtn.addActionListener(e -> executeBackupFlow(false, null, dlg));
        c.add(localOnlyBtn);
        
        c.add(Box.createVerticalStrut(12));

        JButton dualBtn = primaryBtn2("☁ Save Locally & Upload to Drive");
        dualBtn.setBackground(new Color(15, 157, 88)); 
        dualBtn.addActionListener(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty() || email.contains("example") || !email.contains("@")) {
                JOptionPane.showMessageDialog(dlg, "Please enter a valid Gmail address.");
                return;
            }
            executeBackupFlow(true, email, dlg);
        });
        c.add(dualBtn);

        // 6. Final assembly
        dlg.add(c, BorderLayout.CENTER);
        dlg.setVisible(true);
    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "UI Error: " + ex.getMessage());
    }
}
private void executeBackupFlow(boolean useCloud, String email, JDialog dlg) {
    try {
        // 1. Setup Local Path
        String defaultPath = session.getBackupPath();
        String saveDir = (defaultPath != null && !defaultPath.isEmpty()) ? defaultPath : System.getProperty("user.home") + "/Desktop";
        
        // 2. Generate JSON
        BackupManager bm = new BackupManager(activeChannel);
        for (LocalDB.StoredMessage m : LocalDB.getMessages(activeChannel, 10000)) {
            bm.addMessage(m.sender, m.content);
        }
        java.io.File savedFile = bm.exportToJson(saveDir);

        if (useCloud) {
            JOptionPane.showMessageDialog(dlg, "Local copy secured.\nNow initiating Cloud upload for: " + email, "Syncing...", JOptionPane.INFORMATION_MESSAGE);
            // Pass the email to your manager
            GoogleDriveManager.uploadBackupToDrive(savedFile, email); 
        }

        dlg.dispose();
        String msg = useCloud ? "✅ Success! Backup stored locally and on Google Drive." : "✅ Success! JSON stored at: " + savedFile.getAbsolutePath();
        JOptionPane.showMessageDialog(this, msg, "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
        
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Backup Failed", JOptionPane.ERROR_MESSAGE);
    }
}
    private void appendBubble(String sender, String content, String time,
                               boolean isOwn, boolean special) {
        
        // Restore newlines that were sent over the network
        content = content.replace(" // ", "<br>");
                                   
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(special);
        if (special) row.setBackground(MENTION_BG);
        row.setBorder(new EmptyBorder(6, 12, 6, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Color avColor = isOwn ? session.getAvatarColor()
                : new Color(Math.abs(sender.hashCode()) % 180 + 55, 90, 200);
        String initials = sender.length() >= 2 ? sender.substring(0, 2).toUpperCase() : sender.toUpperCase();
        
        // Pass the user profile image if it is own message
        Image imgToRender = isOwn ? userProfileImage : null;
        JPanel av = avatar(initials, avColor, 40, imgToRender);

        JPanel msgBubble = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isOwn ? new Color(88, 101, 242) : new Color(64, 68, 75)); 
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            }
        };
        msgBubble.setOpaque(false);
        msgBubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel msgBox = new JPanel();
        msgBox.setLayout(new BoxLayout(msgBox, BoxLayout.Y_AXIS));
        msgBox.setOpaque(false);

        JPanel headerRow = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 6, 0));
        headerRow.setOpaque(false);
        JLabel nameLbl = new JLabel(sender);
        nameLbl.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        nameLbl.setForeground(isOwn ? Color.WHITE : TEXT_HDR);
        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 11));
        timeLbl.setForeground(new Color(200, 200, 200));

        // Reply button
        JButton replyBtn = ghostBtn2("↩");
        replyBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        replyBtn.setForeground(new Color(180, 180, 180));
        String finalContent = content.replace("<br>", " ");
        replyBtn.addActionListener(e -> {
            String shortContent = finalContent.length() > 30 ? finalContent.substring(0, 30) + "..." : finalContent;
            replyingToMsg = sender + ": " + shortContent;
            replyTextLabel.setText("Replying to " + replyingToMsg);
            replyPanel.setVisible(true);
            chatInput.requestFocus();
        });

        if (isOwn) {
            headerRow.add(timeLbl);
            headerRow.add(nameLbl);
        } else {
            headerRow.add(nameLbl);
            headerRow.add(timeLbl);
            headerRow.add(replyBtn);
        }

        String textAlign = isOwn ? "right" : "left";
        JLabel contentLbl = new JLabel(
            "<html><div style='max-width:450px;word-wrap:break-word;font-family:Segoe UI Symbol;font-size:16pt;color:white;text-align:" + textAlign + "'>"
            + content.replace("<", "&lt;").replace(">", "&gt;")
            + "</div></html>");
        contentLbl.setBorder(new EmptyBorder(4, 0, 0, 0));

        msgBox.add(headerRow);
        msgBox.add(contentLbl);
        msgBubble.add(msgBox, BorderLayout.CENTER);

        JPanel alignmentWrapper = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        alignmentWrapper.setOpaque(false);
        alignmentWrapper.add(msgBubble);

        if (isOwn) {
            row.add(alignmentWrapper, BorderLayout.CENTER);
            row.add(av, BorderLayout.EAST);
        } else {
            row.add(av, BorderLayout.WEST);
            row.add(alignmentWrapper, BorderLayout.CENTER);
        }

        chatPanel.add(row);
        chatPanel.add(Box.createVerticalStrut(6));
        scrollToBottom();
    }

    private void appendFileMsg(String[] parts) {
        JPanel fb = FileShareManager.buildFileBubble(parts,
                parts[1].equalsIgnoreCase(session.getDisplayName()));
        chatPanel.add(fb);
        chatPanel.add(Box.createVerticalStrut(4));
        scrollToBottom();
    }

    private void appendSystemMsg(String msg) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font("Segoe UI Symbol", Font.ITALIC, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl);
        chatPanel.add(row);
        scrollToBottom();
    }

    private void sendSystemMsg(String msg) { appendSystemMsg(msg); }

    private void scrollToBottom() {
        chatPanel.revalidate();
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = chatScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }

    private String now() {
        return java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
    private JPanel friendRow(String username, boolean online) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(BG_CHANNELS);
        row.setBorder(new EmptyBorder(6, 12, 6, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(BG_HOVER); }
            public void mouseExited(MouseEvent e)  { row.setBackground(BG_CHANNELS); }
            public void mouseClicked(MouseEvent e) { openDM(username); }
        });

        Color ac = new Color(Math.abs(username.hashCode()) % 180 + 55, 90, 200);
        String ini = username.length()>=2 ? username.substring(0,2).toUpperCase() : username.toUpperCase();
        JPanel av = avatar(ini, ac, 36, null);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel name = new JLabel(username);
        name.setFont(new Font("Segoe UI Symbol", Font.BOLD, 13));
        name.setForeground(TEXT);
        JLabel status = new JLabel(online ? "● Online" : "○ Offline");
        status.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 11));
        status.setForeground(online ? GREEN : TEXT_MUTED);
        info.add(name); info.add(status);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        btns.setOpaque(false);
        JButton msgBtn = iconBtn("💬");
        msgBtn.setToolTipText("Send DM");
        msgBtn.addActionListener(e -> openDM(username));
        JButton rmBtn = iconBtn("✕");
        rmBtn.setForeground(new Color(180,60,60));
        rmBtn.setToolTipText("Remove");
        rmBtn.addActionListener(e -> { fm.removeFriend(username); showFriendsView(); });
        btns.add(msgBtn); btns.add(rmBtn);

        row.add(av, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private JPanel dmRow(String username) {
        JPanel row = new JPanel(new BorderLayout(8,0));
        row.setBackground(BG_CHANNELS);
        row.setBorder(new EmptyBorder(4, 16, 4, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(BG_HOVER); }
            public void mouseExited(MouseEvent e)  { row.setBackground(BG_CHANNELS); }
            public void mouseClicked(MouseEvent e) { openDM(username); }
        });
        Color ac = new Color(Math.abs(username.hashCode()) % 180 + 55, 90, 200);
        String ini = username.length()>=2 ? username.substring(0,2).toUpperCase() : username.toUpperCase();
        row.add(avatar(ini, ac, 26, null), BorderLayout.WEST);
        JLabel lbl = new JLabel(" " + username);
        lbl.setForeground(TEXT_MUTED);
        lbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    private JPanel pendingRow(String username) {
        JPanel row = new JPanel(new BorderLayout(10,0));
        row.setBackground(BG_CHANNELS);
        row.setBorder(new EmptyBorder(6,12,6,12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JLabel name = new JLabel(username + "  (pending)");
        name.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
        name.setForeground(TEXT_MUTED);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT,4,0));
        btns.setOpaque(false);
        JButton accept = smallBtn("✓ Accept", GREEN);
        JButton decline = smallBtn("✕ Decline", RED);
        accept.addActionListener(e -> { 
            fm.acceptRequest(username); 
            sendRaw("/acceptfriend " + username);
            showFriendsView(); 
        });
        decline.addActionListener(e -> { fm.declineRequest(username); showFriendsView(); });
        btns.add(accept); btns.add(decline);

        row.add(name, BorderLayout.WEST);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private JPanel groupRow(String name) {
        JPanel row = new JPanel(new BorderLayout(8,0));
        row.setBackground(BG_CHANNELS);
        row.setBorder(new EmptyBorder(4,12,4,12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(BG_HOVER); }
            public void mouseExited(MouseEvent e)  { row.setBackground(BG_CHANNELS); }
            public void mouseClicked(MouseEvent e) { openGroup(name); }
        });
        JLabel lbl = new JLabel("# " + name);
        lbl.setForeground(TEXT_MUTED);
        lbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }

    private JPanel memberRow(String username) {
        JPanel row = new JPanel(new BorderLayout(8,0));
        row.setBackground(BG_CHANNELS);
        row.setBorder(new EmptyBorder(4,16,4,16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        Color ac = new Color(Math.abs(username.hashCode()) % 180 + 55, 90, 200);
        String ini = username.length()>=2 ? username.substring(0,2).toUpperCase() : username.toUpperCase();
        JLabel lbl = new JLabel(" " + username);
        lbl.setForeground(TEXT_MUTED);
        lbl.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
        row.add(avatar(ini, ac, 26, null), BorderLayout.WEST);
        row.add(lbl, BorderLayout.CENTER);
        if (!fm.isFriend(username) && !username.equals(session.getDisplayName())) {
            JButton addBtn = smallBtn("＋", BLURPLE);
            addBtn.setToolTipText("Add Friend");
            addBtn.addActionListener(e -> { fm.addFriend(username); showFriendsView(); });
            row.add(addBtn, BorderLayout.EAST);
        }
        return row;
    }

    // UPDATED AVATAR WITH CIRCULAR IMAGE SUPPORT
    private JPanel avatar(String initials, Color bg, int size, Image profileImg) {
        JPanel p = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (profileImg != null) {
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImg, 0, 0, size, size, null);
                } else {
                    g2.setColor(bg);
                    g2.fillOval(0,0,size,size);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,size/3));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(initials,(size-fm.stringWidth(initials))/2,
                        (size+fm.getAscent()-fm.getDescent())/2);
                }
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(size,size));
        p.setMinimumSize(new Dimension(size,size));
        p.setMaximumSize(new Dimension(size,size));
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI Symbol",Font.BOLD,11));
        l.setForeground(TEXT_MUTED);
        l.setBorder(new EmptyBorder(14,12,4,12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

  private JLabel mutedNote(String text) {
    JLabel l = new JLabel("  " + text);
    l.setFont(new Font("Segoe UI", Font.ITALIC, 12));
    l.setForeground(new Color(90, 95, 105));
    l.setBorder(new EmptyBorder(4, 12, 4, 12));
    l.setAlignmentX(Component.LEFT_ALIGNMENT); // Keeps text flush to the left
    return l;
}
    private JButton iconBtn(String icon) {
        JButton b = new JButton(icon);
        b.setFont(new Font("Segoe UI Symbol",Font.PLAIN,16));
        b.setForeground(TEXT_MUTED);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4,6,4,6));
        return b;
    }

    private JButton inputIconBtn(String icon) {
        JButton b = new JButton(icon);
        b.setFont(new Font("Segoe UI Symbol",Font.PLAIN,18));
        b.setForeground(TEXT_MUTED);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(44,44));
        return b;
    }

    private JButton smallBtn(String text, Color color) {
        JButton btn = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,11));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(Math.max(60, text.length()*8), 26));
        return btn;
    }

    private JButton ctrlBtn(String text, Color color) {
        JButton btn = new JButton(text) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),4,4);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,10));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(28, 20));
        return btn;
    }

    private JPanel railIcon(String label, String tooltip, Color color) {
        JPanel p = new JPanel() {
            boolean hover = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
                public void mouseExited(MouseEvent e)  { hover=false; repaint(); }
            }); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int m = hover ? 6:12; int sz = getWidth()-m*2;
                g2.setColor(hover ? color.brighter() : color);
                if (hover) g2.fillRoundRect(m,(getHeight()-sz)/2,sz,sz,16,16);
                else       g2.fillOval(m,(getHeight()-sz)/2,sz,sz);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,label.length()>2?11:16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label,(getWidth()-fm.stringWidth(label))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(72,52));
        p.setMaximumSize(new Dimension(72,52));
        p.setToolTipText(tooltip);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return p;
    }

    private JPanel railDiv() {
        JPanel d = new JPanel() {
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(255,255,255,18));
                g.fillRect(16, 4, getWidth()-32, 1);
            }
        };
        d.setOpaque(false);
        d.setPreferredSize(new Dimension(72,10));
        d.setMaximumSize(new Dimension(72,10));
        return d;
    }

    private JDialog dialog(String title, int w, int h) {
        JDialog dlg = new JDialog(this, title, true);
        dlg.setSize(w, h);
        dlg.setLocationRelativeTo(this);
        dlg.setUndecorated(true);
        dlg.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(255,255,255,18), 1));
        return dlg;
    }

    private JPanel dialogContent() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CHANNELS);
        p.setBorder(new EmptyBorder(22,26,22,26));
        return p;
    }

    private void header(JPanel c, String title, String sub, JDialog dlg) {
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI Symbol", Font.BOLD, 22));
        t.setForeground(TEXT_HDR);
        topRow.add(t, BorderLayout.WEST);
        // Add the Close Button
        JButton closeBtn = ghostBtn2("X");
        closeBtn.setPreferredSize(new Dimension(40, 30));
        closeBtn.addActionListener(e -> dlg.dispose());
        topRow.add(closeBtn, BorderLayout.EAST);
        c.add(topRow);
        if (!sub.isEmpty()) {
            JLabel s = new JLabel(sub);
            s.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 13));
            s.setForeground(TEXT_MUTED);
            s.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.add(Box.createVerticalStrut(4));
            c.add(s);
        }
    }

    private JTextField dlgField(String placeholder) {
        JTextField f = new JTextField() {
            protected void paintComponent(Graphics g) {
                ((Graphics2D)g).setColor(new Color(28,29,32));
                ((Graphics2D)g).fillRoundRect(0,0,getWidth(),getHeight(),5,5);
                super.paintComponent(g);
            }
        };
        f.setOpaque(false); f.setBackground(new Color(28,29,32));
        f.setForeground(TEXT); f.setCaretColor(BLURPLE);
        f.setFont(new Font("Segoe UI Symbol",Font.PLAIN,14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255,255,255,18),1,true),
            new EmptyBorder(10,12,10,12)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setText(placeholder); f.setForeground(TEXT_MUTED);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_MUTED); }
            }
        });
        return f;
    }

    private JLabel dlgLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI Symbol",Font.BOLD,11));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JButton primaryBtn2(String text) {
        JButton btn = new JButton(text) {
            boolean hv = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hv=true; repaint(); }
                public void mouseExited(MouseEvent e)  { hv=false; repaint(); }
            }); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hv ? BLURPLE_DK : BLURPLE);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),6,6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Symbol",Font.BOLD,13));
                FontMetrics fm=g2.getFontMetrics();
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        return btn;
    }

    private JButton ghostBtn2(String text) {
        JButton b = new JButton(text);
        b.setForeground(TEXT_MUTED);
        b.setFont(new Font("Segoe UI Symbol",Font.PLAIN,13));
        b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        return b;
    }

    private MouseAdapter click(Runnable r) {
        return new MouseAdapter() { public void mouseClicked(MouseEvent e) { r.run(); } };
    }
}