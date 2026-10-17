package threadtalk;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private final Connection connection;

    public Database(String dbPath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initSchema();
    }

    private void initSchema() throws SQLException {
        String query = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp TEXT NOT NULL
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(query);
        }
    }

    public synchronized void saveMessage(Message message) throws SQLException {
        String query = """
                INSERT INTO messages(username, content, timestamp) VALUES (?, ?, ?);
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, message.getUsername());
            preparedStatement.setString(2, message.getContent());
            preparedStatement.setString(3, message.getTimestamp().toString());

            preparedStatement.executeUpdate();
        }
    }

    public synchronized List<Message> loadAllMessages() throws SQLException {
        String query = """
                SELECT id, username, content, timestamp FROM messages ORDER BY id ASC;
                """;

        List<Message> result = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery();) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String content = resultSet.getString("content");
                LocalDateTime timestamp = LocalDateTime.parse(resultSet.getString("timestamp"));

                Message message = new Message(id, username, content, timestamp);

                result.add(message);
            }

            return result;
        }
    }
}
