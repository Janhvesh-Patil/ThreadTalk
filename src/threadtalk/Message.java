package threadtalk;
import java.time.LocalDateTime;

public class Message {
    private final int id;
    private final String username;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(int id, String username, String content, LocalDateTime timestamp) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}