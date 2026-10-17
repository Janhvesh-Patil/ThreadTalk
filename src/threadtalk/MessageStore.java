package threadtalk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageStore {
    private final List<Message> messages = new ArrayList<>();
    private int lastId = 0;

    public Message addMessage(String username, String content) {
        Message message = new Message(lastId, username, content, LocalDateTime.now());
        messages.add(message);
        lastId++;
        return message;
    }

    public List<Message> getMessagesSince(int fromIndex) {
        if (fromIndex < 0 || fromIndex >= messages.size()) return new ArrayList<>();
        return List.copyOf(messages.subList(fromIndex, messages.size()));
    }

    public List<Message> getAllMessages() {
        return List.copyOf(messages);
    }
}