package threadtalk;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageStore messageStore;
    private final Database database;

    public ClientHandler(Socket socket, MessageStore messageStore, Database database) {
        this.socket = socket;
        this.messageStore = messageStore;
        this.database = database;
    }

    @Override
    public void run() {
        try (Socket socket = this.socket;
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),
                     StandardCharsets.UTF_8), true);) {

            HttpUtils httpUtils = new HttpUtils();
            httpUtils.parser(bufferedReader);
            String response = route(httpUtils);

            printWriter.print(response);
            printWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String route(HttpUtils httpUtils) {
        if (httpUtils.getMethod().isEmpty()) {
            return HttpUtils.buildErrorResponse(400, "No HTTP method was called");
        }

        if (httpUtils.getMethod().equals("OPTIONS")) {
            return "HTTP/1.1 204 No Content\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                    "\r\n";
        }

        String method = httpUtils.getMethod();
        String path = httpUtils.getPath();

        switch (method) {
            case "GET" -> {
                if (path.equals("/")) {
                    try {
                        return HttpUtils.buildHtmlResponse(readFile("frontend/index.html"));
                    } catch (IOException e) {
                        return HttpUtils.buildErrorResponse(500, "Could not read file");
                    }
                } else if (path.equals("/style.css")) {
                    try {
                        return HttpUtils.buildTextResponse(
                                readFile("frontend/style.css"),
                                "text/css"
                        );
                    } catch (IOException e) {
                        return HttpUtils.buildErrorResponse(500, "Could not read CSS file");
                    }
                } else if (path.equals("/script.js")) {
                    try {
                        return HttpUtils.buildTextResponse(readFile("frontend/script.js"),
                                "application/javascript");
                    } catch (IOException e) {
                        return HttpUtils.buildErrorResponse(500, "Could not read JavaScript file");
                    }
                } else if (path.equals("/messages")) {
                    List<Message> messages = messageStore.getAllMessages();
                    String jsonMessages = HttpUtils.messagesToJson(messages);
                    return HttpUtils.buildJsonResponse(jsonMessages);
                }
            } case "POST" -> {
                if (path.equals("/send")) {
                    String[] body = httpUtils.getBody().split("&");
                    String username = URLDecoder.decode(body[0].split("=")[1], StandardCharsets.UTF_8);
                    String content = URLDecoder.decode(body[1].split("=")[1], StandardCharsets.UTF_8);
                    Message message = messageStore.addMessage(username, content);
                    try {
                        database.saveMessage(message);
                    } catch (SQLException e) {
                        return HttpUtils.buildErrorResponse(500, "Could not save message");
                    }
                    return HttpUtils.buildJsonResponse("Message sent successfully");
                } else {
                    return HttpUtils.buildErrorResponse(404, "An error occurred when sending the message");
                }
            } default -> {
                return HttpUtils.buildErrorResponse(404, "An error occurred when sending the message");
            }
        }

        return HttpUtils.buildErrorResponse(400, "Bad request received");
    }

    private String readFile(String filePath) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder stringBuilder = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }

            return stringBuilder.toString();
        }
    }
}
