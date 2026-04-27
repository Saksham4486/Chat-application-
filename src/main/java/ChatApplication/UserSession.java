package ChatApplication;

import java.awt.Color;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String displayName;
    private String email;
    private String backupPath;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void set(String username, String displayName, String email) {
        this.username    = username;
        this.displayName = displayName;
        this.email       = email;
        LocalDB.saveSession(username);
    }

    public String getUsername()    { return username; }
    public String getDisplayName() { return displayName != null ? displayName : username; }
    public String getEmail()       { return email != null ? email : ""; }
    
    public String getBackupPath() { return backupPath; }
    public void setBackupPath(String backupPath) { this.backupPath = backupPath; }

    public void logout() {
        LocalDB.clearSession();
        username = null; displayName = null; email = null; backupPath = null;
    }

    public boolean isLoggedIn() { return username != null; }

    public String getInitials() {
        String n = getDisplayName();
        String[] parts = n.trim().split("\\s+");
        if (parts.length == 1) return n.substring(0, Math.min(2, n.length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length-1].charAt(0)).toUpperCase();
    }

    public Color getAvatarColor() {
        int h = Math.abs(getDisplayName().hashCode());
        int[][] colors = {
            {88, 101, 242}, {87, 242, 135}, {250, 166, 26},
            {235, 69, 158}, {0, 185, 194},  {255, 115, 100}
        };
        int[] c = colors[h % colors.length];
        return new Color(c[0], c[1], c[2]);
    }
}