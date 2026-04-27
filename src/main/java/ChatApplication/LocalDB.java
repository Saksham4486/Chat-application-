package ChatApplication;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class LocalDB {
    private static final String DATA_DIR =
            System.getProperty("user.home") + File.separator + ".chatapp";

    static {
        new File(DATA_DIR).mkdirs();
    }

    private static File f(String name) {
        return new File(DATA_DIR + File.separator + name);
    }
    public static void saveServerIp(String ip) {
        writeLines("server_ip.dat", List.of(ip));
    }

    public static String loadServerIp() {
        try {
            List<String> lines = readLines("server_ip.dat");
            if (!lines.isEmpty() && !lines.get(0).isBlank()) return lines.get(0).trim();
        } catch (Exception ignored) {}
        return "localhost"; // default fallback
    }
    public static boolean userExists(String username) {
        try {
            for (String line : readLines("users.dat")) {
                if (line.split("\\|")[0].equalsIgnoreCase(username)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
    public static boolean registerUser(String username, String password,
                                       String displayName, String email) {
        if (userExists(username)) return false;
        String hash = hashPassword(password);
        String line = username + "|" + hash + "|" + displayName + "|" + email;
        appendLine("users.dat", line);
        return true;
    }
    public static UserRecord loginUser(String username, String password) {
        String hash = hashPassword(password);
        try {
            for (String line : readLines("users.dat")) {
                String[] p = line.split("\\|", 4);
                if (p.length >= 2 && p[0].equalsIgnoreCase(username) && p[1].equals(hash)) {
                    return new UserRecord(
                        p[0],
                        p.length > 2 ? p[2] : p[0],
                        p.length > 3 ? p[3] : ""
                    );
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static class UserRecord {
        public final String username, displayName, email;
        public UserRecord(String u, String d, String e) {
            username = u; displayName = d; email = e;
        }
    }
    
    // --- NEW BACKUP METHODS ---
    public static void saveBackupPath(String username, String path) {
        writeLines("backup_" + username.toLowerCase() + ".dat", List.of(path));
    }

    public static String loadBackupPath(String username) {
        try {
            List<String> lines = readLines("backup_" + username.toLowerCase() + ".dat");
            if (!lines.isEmpty() && !lines.get(0).isBlank()) return lines.get(0).trim();
        } catch (Exception ignored) {}
        return null;
    }
    // --------------------------

    public static List<String> getFriends(String username) {
        List<String> list = new ArrayList<>();
        try {
            for (String line : readLines("friends_" + username.toLowerCase() + ".dat")) {
                if (!line.isBlank()) list.add(line.trim());
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void addFriend(String username, String friend) {
        List<String> current = getFriends(username);
        if (!current.contains(friend)) {
            appendLine("friends_" + username.toLowerCase() + ".dat", friend);
        }
    }

    public static void removeFriend(String username, String friend) {
        List<String> list = getFriends(username);
        list.remove(friend);
        writeLines("friends_" + username.toLowerCase() + ".dat", list);
    }
    public static Map<String, List<String>> getAllGroups() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        try {
            for (String line : readLines("groups.dat")) {
                String[] p = line.split("\\|", 2);
                if (p.length == 2) {
                    List<String> members = new ArrayList<>(Arrays.asList(p[1].split(",")));
                    members.removeIf(String::isBlank);
                    map.put(p[0], members);
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    public static List<String> getUserGroups(String username) {
        List<String> groups = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : getAllGroups().entrySet()) {
            if (e.getValue().contains(username)) groups.add(e.getKey());
        }
        return groups;
    }

    public static void createGroup(String name, String creator) {
        Map<String, List<String>> groups = getAllGroups();
        if (!groups.containsKey(name)) {
            groups.put(name, new ArrayList<>(List.of(creator)));
            saveAllGroups(groups);
        }
    }

    public static void addMemberToGroup(String groupName, String username) {
        Map<String, List<String>> groups = getAllGroups();
        groups.computeIfAbsent(groupName, k -> new ArrayList<>());
        if (!groups.get(groupName).contains(username)) {
            groups.get(groupName).add(username);
            saveAllGroups(groups);
        }
    }

    private static void saveAllGroups(Map<String, List<String>> groups) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            lines.add(e.getKey() + "|" + String.join(",", e.getValue()));
        }
        writeLines("groups.dat", lines);
    }

    public static void saveMessage(String channel, String sender, String content) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (content.length() > 500) {
            content = "[File sent]";
        }
        appendLine("msgs_" + sanitize(channel) + ".dat", ts + "|" + sender + "|" + content);
    }

    public static List<StoredMessage> getMessages(String channel, int limit) {
        List<StoredMessage> all = new ArrayList<>();
        try {
            List<String> lines = readLines("msgs_" + sanitize(channel) + ".dat");
            int start = Math.max(0, lines.size() - limit);
            for (int i = start; i < lines.size(); i++) {
                String[] p = lines.get(i).split("\\|", 3);
                if (p.length == 3) all.add(new StoredMessage(p[0], p[1], p[2]));
            }
        } catch (Exception ignored) {}
        return all;
    }

    public static class StoredMessage {
        public final String timestamp, sender, content;
        public StoredMessage(String t, String s, String c) {
            timestamp = t; sender = s; content = c;
        }
    }
    public static void saveSession(String username) {
        writeLines("session.dat", List.of(username));
    }

    public static String loadSession() {
        try {
            List<String> lines = readLines("session.dat");
            if (!lines.isEmpty() && !lines.get(0).isBlank()) return lines.get(0).trim();
        } catch (Exception ignored) {}
        return null;
    }

    public static void clearSession() {
        writeLines("session.dat", List.of());
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // fallback
        }
    }

    private static List<String> readLines(String fileName) throws IOException {
        File f = f(fileName);
        if (!f.exists()) return new ArrayList<>();
        return new ArrayList<>(Files.readAllLines(f.toPath()));
    }

    private static void appendLine(String fileName, String line) {
        try (FileWriter fw = new FileWriter(f(fileName), true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(line);
        } catch (IOException e) {
            System.err.println("DB write error: " + e.getMessage());
        }
    }

    private static void writeLines(String fileName, List<String> lines) {
        try { Files.write(f(fileName).toPath(), lines); }
        catch (IOException e) { System.err.println("DB write error: " + e.getMessage()); }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }

    public static String getDataDir() { return DATA_DIR; }
}