package ChatApplication;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class RestoreManager {
    private String backupFilePath;
    private List<Message> restoredMessages;

    public RestoreManager(String backupFilePath)
    {
    this.backupFilePath = backupFilePath;
    this.restoredMessages = new ArrayList<>();
    System.out.println("RestoreManager initialized for: " + backupFilePath);
    }

    public List<Message> restoreFromTxt() throws IOException
    {
        restoredMessages.clear();

        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(backupFilePath)))
        {
            bufferedReader.readLine();
            bufferedReader.readLine(); 
            bufferedReader.readLine(); 
            String line;
            while((line = bufferedReader.readLine()) != null)
            {
                String timestamp = line.substring(1, line.indexOf("]"));
                String remaining = line.substring(line.indexOf("]") + 2);
                String sender = remaining.substring(0, remaining.indexOf(":"));
                String content = remaining.substring(remaining.indexOf(":") + 2);
                Message message = new Message(sender, content);
                restoredMessages.add(message);
            }
        }

        System.out.println("Restored " + restoredMessages.size() + " messages from: " + backupFilePath);
        return restoredMessages;
    }

    public List<Message> restoreFromJson() throws IOException
{
    restoredMessages.clear();

    try(BufferedReader bufferedReader = new BufferedReader(new FileReader(backupFilePath)))
    {
        String sender = null;
        String content = null;
        String timestamp = null;
        String line;

        while((line = bufferedReader.readLine()) != null)
        {
            line = line.trim();

            if(line.contains("\"sender\""))
            {
                String[] parts = line.split(":", 2);
                sender = parts[1];
                sender = sender.trim();
                sender = sender.replace("\"", "");
                sender = sender.replace(",", "");
            }
            else if(line.contains("\"content\""))
            {
                String[] parts = line.split(":", 2);
                content = parts[1];
                content = content.trim();
                content = content.replace("\"", "");
                content = content.replace(",", "");
            }
            else if(line.contains("\"timestamp\""))
            {
                String[] parts = line.split(":", 2);  
                timestamp = parts[1];
                timestamp = timestamp.trim();
                timestamp = timestamp.replace("\"", "");
                timestamp = timestamp.replace(",", "");
                if(sender != null && content != null)
                {
                    Message message = new Message(sender, content);
                    restoredMessages.add(message);
                    sender = null;
                    content = null;
                    timestamp = null;
                }
            }
        }
    }

    System.out.println("Restored " + restoredMessages.size() + " messages from: " + backupFilePath);
    return restoredMessages;
}

    public void printMessages()
{
    if(restoredMessages.isEmpty())
    {
        System.out.println("No messages to display.");
        return;
    }

    System.out.println("================================");
    System.out.println("Restored Chat History");
    System.out.println("================================");

    for(Message message : restoredMessages)
    {
        System.out.println(message.toBackupString());
    }

    System.out.println("================================");
    System.out.println("Total messages: " + restoredMessages.size());
}
}

