package ChatApplication;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BackupManager {
    private List<Message> messages;
    private String chatRoomName;

    public BackupManager(String chatRoomName) {
        this.chatRoomName = chatRoomName;
        this.messages = new ArrayList<>();
    }

    public void addMessage(String sender, String content) {
        Message message = new Message(sender, content);
        messages.add(message);
        System.out.println("Message saved to backup: " + message.toBackupString());
    }

    private String getBackupFileName(String extension) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return chatRoomName + "_backup_" + timestamp + "." + extension;
    }

    public void exportToTxt() throws IOException {
        String fileName = getBackupFileName("txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write("Chat Backup: " + chatRoomName);
            bw.newLine();
            bw.write("Total Messages: " + messages.size());
            bw.newLine();
            bw.write("================================");
            bw.newLine();
            for (Message message : messages) {
                bw.write(message.toBackupString());
                bw.newLine();
            }
        }
        System.out.println("Chat exported to: " + fileName);
    }

    public java.io.File exportToJson(String directoryPath) throws IOException {
        java.io.File dir = new java.io.File(directoryPath);
        if (!dir.exists()) dir.mkdirs();
        
        String fileName = getBackupFileName("json");
        java.io.File outFile = new java.io.File(dir, fileName);
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            bw.write(toJsonString());
        }
        System.out.println("Chat exported to: " + outFile.getAbsolutePath());
        return outFile; 
    }
    /** Returns the full JSON string (used for cloud backup) */
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"chatRoom\": \"").append(escape(chatRoomName)).append("\",\n");
        sb.append("  \"totalMessages\": ").append(messages.size()).append(",\n");
        sb.append("  \"exportedAt\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        sb.append("  \"messages\": [\n");

        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            sb.append("    {\n");
            sb.append("      \"sender\": \"").append(escape(m.getSender())).append("\",\n");
            sb.append("      \"content\": \"").append(escape(m.getContent())).append("\",\n");
            sb.append("      \"timestamp\": \"").append(m.getTimestamp()).append("\"\n");
            sb.append("    }");
            if (i < messages.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  ]\n}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public void loadFromDatabase() {
        try {
            MessageDAO messageDAO = new MessageDAO();
            List<String> history = messageDAO.getChatHistory();
            messages.clear();
            for (String entry : history) {
                if (entry.contains(":")) {
                    String[] parts = entry.split(":", 2);
                    messages.add(new Message(parts[0].trim(), parts[1].trim()));
                }
            }
            System.out.println("Loaded " + messages.size() + " messages from database.");
        } catch (Exception e) {
            System.out.println("Database load failed: " + e.getMessage());
        }
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
