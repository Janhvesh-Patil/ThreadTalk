package threadtalk;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpUtils {
    private String method;
    private String path;
    private String body;
    private Map<String, String> header = new HashMap<>();
    private int contentLength;

    public void parser(BufferedReader reader) throws IOException {
        //method and path
        String firstLine = reader.readLine();

        if (firstLine == null || firstLine.isBlank()) {
            method = "";
            path = "";
            return;
        }

        String[] methodAndPath = firstLine.split(" ");
        method = methodAndPath[0];
        path = methodAndPath[1];

        //headers
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String[] keyValuePair = line.split(":", 2);
            header.put(keyValuePair[0].trim(), keyValuePair[1].trim());
        }

        String cl = header.get("Content-Length");
        contentLength = (cl == null) ? 0 : Integer.parseInt(cl);

        //body
        char[] bodyChars = new char[contentLength];
        int totalRead = 0;

        while (totalRead < contentLength) {
            int read = reader.read(bodyChars, totalRead, contentLength - totalRead);
            if (read == -1) break;
            totalRead += read;
        }

        body = new String(bodyChars, 0, totalRead);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getBody() {
        return body;
    }

    public static String buildJsonResponse(String jsonBody) {
        return "HTTP/1.1 200 OK" +  "\r\n"
                //Headers
                + "Content-Type: application/json" + "\r\n" +
                "Content-Length: " + jsonBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Access-Control-Allow-Origin: *" + "\r\n" +
                //Body
                "\r\n" +
                jsonBody;
    }

    public static String buildHtmlResponse(String htmlBody) {
        return "HTTP/1.1 200 OK" +  "\r\n"
                //Headers
                + "Content-Type: text/html" + "\r\n" +
                "Content-Length: " + htmlBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Access-Control-Allow-Origin: *" + "\r\n" +
                //Body
                "\r\n" +
                htmlBody;
    }

    public static String buildErrorResponse(int statusCode, String message) {
        return "HTTP/1.1 " + statusCode + " " +
                switch (statusCode) {
                    case 400 -> "Bad Request";
                    case 404 -> "Not Found";
                    default -> "Unknown";
                } + "\r\n" +
                "Content-Type: text/plain" + "\r\n" +
                "Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                //Body
                "\r\n" +
                message;
    }

    public static String messagesToJson(List<Message> messages) {
        String elements = messages.stream()
                .map(m -> "{" +
                        "\"id\":" + m.getId() + "," +
                        "\"username\":\"" + escapeJson(m.getUsername()) + "\"," +
                        "\"content\":\"" + escapeJson(m.getContent()) + "\"," +
                        "\"timestamp\":\"" + m.getTimestamp() + "\"" +
                        "}")
                .collect(Collectors.joining(","));

        return "[" + elements + "]";
    }


    public static String buildTextResponse(String content, String contentType) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "; charset=UTF-8\r\n" +
                "Content-Length: " +
                content.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "\r\n" +
                content;
    }


    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
