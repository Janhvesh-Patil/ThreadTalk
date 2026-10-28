package threadtalk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageStore {
    private final List<Message> messages = new ArrayList<>();
    private int lastId = 0;

    public synchronized Message addMessage(String username, String content) {
        Message message = new Message(lastId, username, content, LocalDateTime.now());
        messages.add(message);
        lastId++;
        return message;
    }

    public synchronized List<Message> getMessagesSince(int fromIndex) {
        if (fromIndex < 0 || fromIndex >= messages.size()) return new ArrayList<>();
        return List.copyOf(messages.subList(fromIndex, messages.size()));
    }

    public synchronized void loadMessage(Message message) {
        messages.add(message);
        lastId = message.getId() + 1;
    }

    public synchronized List<Message> getAllMessages() {
        return List.copyOf(messages);
    }
}