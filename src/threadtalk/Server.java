package threadtalk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8081;

    public static void main(String[] args) {
        MessageStore messageStore = new MessageStore();
        Database database;
        try {
            database = new Database("data/chat.db");
        } catch (SQLException e) {
            System.err.println("Failed to open database: " + e.getMessage());
            return;
        }

        try (ExecutorService executorService = Executors.newFixedThreadPool(7);
             ServerSocket serverSocket = new ServerSocket(PORT)) {

            List<Message> allMessages = database.loadAllMessages();
            allMessages.forEach(m -> messageStore.addMessage(m.getUsername(), m.getContent()));

            while (true) {
                Socket socket = serverSocket.accept();
                executorService.submit(new ClientHandler(socket, messageStore, database));
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
