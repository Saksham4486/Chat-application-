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

    public BackupManager(String chatRoomName)
    {
        this.chatRoomName = chatRoomName;
        this.messages = new ArrayList<>();
    }

    public void addMessage(String sender, String content)
    {
        Message message = new Message(sender, content);
        messages.add(message);
        System.out.println("Message saved to backup: " + message.toBackupString());
    }

    private String getBackupFileName(String extension)
    {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return chatRoomName + "_backup_" + timestamp + "." + extension;
    }

    public void exportToTxt() throws IOException
    {
        String fileName = getBackupFileName("txt");
        FileWriter fileWriter = new FileWriter(fileName);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        bufferedWriter.write("Chat Backup: " + chatRoomName);
        bufferedWriter.newLine();
        bufferedWriter.write("Total Messages: " + messages.size());
        bufferedWriter.newLine();
        bufferedWriter.write("================================");
        bufferedWriter.newLine();

        for(Message message : messages)
        {
            bufferedWriter.write(message.toBackupString());
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
        System.out.println("Chat exported to: " + fileName);
    }

    public void exportToJson() throws IOException
    {
        String fileName = getBackupFileName("json");
        FileWriter fileWriter = new FileWriter(fileName);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        bufferedWriter.write("{");
        bufferedWriter.newLine();
        bufferedWriter.write("  \"chatRoom\": \"" + chatRoomName + "\",");
        bufferedWriter.newLine();
        bufferedWriter.write("  \"totalMessages\": " + messages.size() + ",");
        bufferedWriter.newLine();
        bufferedWriter.write("  \"messages\": [");
        bufferedWriter.newLine();

        for(int i = 0; i < messages.size(); i++)
        {
            Message message = messages.get(i);
            bufferedWriter.write("    {");
            bufferedWriter.newLine();
            bufferedWriter.write("      \"sender\": \"" + message.getSender() + "\",");
            bufferedWriter.newLine();
            bufferedWriter.write("      \"content\": \"" + message.getContent() + "\",");
            bufferedWriter.newLine();
            bufferedWriter.write("      \"timestamp\": \"" + message.getTimestamp() + "\"");
            bufferedWriter.newLine();

            if(i < messages.size() - 1)
                bufferedWriter.write("    },");
            else
                bufferedWriter.write("    }");

            bufferedWriter.newLine();
        }

        bufferedWriter.write("  ]");
        bufferedWriter.newLine();
        bufferedWriter.write("}");
        bufferedWriter.newLine();

        bufferedWriter.flush();
        bufferedWriter.close();
        System.out.println("Chat exported to: " + fileName);
    }
    public void loadFromDatabase()
{
    try
    {
        MessageDAO messageDAO = new MessageDAO();
        List<String> history = messageDAO.getChatHistory();

        messages.clear();
        System.out.println("Loading messages from database...");

        for(String entry : history)
        {
            if(entry.contains(":"))
            {
                String[] parts = entry.split(":", 2);
                String sender = parts[0].trim();
                String content = parts[1].trim();
                Message message = new Message(sender, content);
                messages.add(message);
            }
        }

        System.out.println("Loaded " + messages.size() + " messages from database.");
    }
    catch(Exception e)
    {
        System.out.println("Database load failed: " + e.getMessage());
        System.out.println("Using messages from memory instead.");
    }
}

}
