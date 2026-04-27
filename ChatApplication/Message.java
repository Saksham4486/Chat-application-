package ChatApplication;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String sender;
    private String content;
    private String timestamp;
    
    public Message(String sender, String content)
    {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }

    public String toBackupString()
    {
        return "[" + timestamp + "] " + sender + ": " + content;
    }
}  
