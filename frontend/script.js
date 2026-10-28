
"use strict";

// 1. Backend endpoints
const API = {
    messages: "/messages",
    send: "/send"
};

// 2. Polling interval: 2000 milliseconds = 2 seconds
const POLL_INTERVAL = 2000;

// 3. Get references to HTML elements
const messageList = document.getElementById("messages");
const messageForm = document.getElementById("message-form");
const usernameInput = document.getElementById("username");
const contentInput = document.getElementById("content");
const sendButton = document.getElementById("send-button");
const sendLabel = document.getElementById("send-label");
const formStatus = document.getElementById("form-status");
const connectionStatus = document.getElementById("connection-status");
const statusDot = document.getElementById("status-dot");

// Prevent overlapping polling requests and duplicate submissions.
let isPolling = false;
let isSending = false;

// Remember the last response so we don't rebuild an unchanged message list.
let lastMessagesJSON = null;

// 4. Remember the username across page reloads.
usernameInput.value = localStorage.getItem("threadtalk-username") || "";

usernameInput.addEventListener("input", () => {
    localStorage.setItem("threadtalk-username", usernameInput.value);
});

// 5. Update the connection indicator.
function updateConnectionStatus(connected) {
    if (connectionStatus) {
        connectionStatus.textContent = connected
            ? "Connected"
            : "Reconnecting";
    }

    if (statusDot) {
        statusDot.classList.toggle("bg-emerald-500", connected);
        statusDot.classList.toggle("bg-amber-500", !connected);
    }
}

// 6. Format the timestamp received from Java.
function formatTimestamp(timestamp) {
    if (!timestamp) return "";

    const date = new Date(timestamp);

    // Fall back to the original value if the date is invalid.
    if (Number.isNaN(date.getTime())) {
        return timestamp;
    }

    return date.toLocaleString([], {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

// 7. Create one message element.
function createMessageElement(message) {
    const article = document.createElement("article");
    article.className = "message-entry flex items-start gap-3";

    const avatar = document.createElement("div");
    avatar.className =
        "flex h-9 w-9 shrink-0 items-center justify-center " +
        "rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700";

    const username = String(message.username ?? "Unknown");
    const content = String(message.content ?? "");

    avatar.textContent = username.trim().charAt(0).toUpperCase() || "?";

    const body = document.createElement("div");
    body.className = "min-w-0 flex-1";

    const metadata = document.createElement("div");
    metadata.className = "mb-1 flex items-baseline gap-2";

    const name = document.createElement("span");
    name.className = "text-sm font-semibold text-gray-900";
    name.textContent = username;

    const time = document.createElement("time");
    time.className = "text-xs text-gray-500";
    time.textContent = formatTimestamp(message.timestamp);

    if (message.timestamp) {
        time.dateTime = message.timestamp;
    }

    const bubble = document.createElement("div");
    bubble.className =
        "message-content inline-block max-w-full rounded-2xl " +
        "rounded-tl-md bg-gray-100 px-4 py-2.5 " +
        "text-sm leading-6 text-gray-800";

    bubble.textContent = content;

    metadata.appendChild(name);
    metadata.appendChild(time);

    body.appendChild(metadata);
    body.appendChild(bubble);

    article.appendChild(avatar);
    article.appendChild(body);

    return article;
}

// 8. Render all messages returned by the server.
function renderMessages(messages) {
    const messagesJSON = JSON.stringify(messages);

    // If nothing changed, leave the existing DOM alone.
    if (messagesJSON === lastMessagesJSON) {
        return;
    }

    const wasNearBottom =
        messageList.scrollHeight -
        messageList.scrollTop -
        messageList.clientHeight < 100;

    const fragment = document.createDocumentFragment();

    if (messages.length === 0) {
        const emptyMessage = document.createElement("p");

        emptyMessage.className =
            "py-16 text-center text-sm text-gray-500";

        emptyMessage.textContent =
            "No messages yet. Start the conversation!";

        fragment.appendChild(emptyMessage);
    } else {
        messages.forEach((message) => {
            fragment.appendChild(createMessageElement(message));
        });
    }

    messageList.replaceChildren(fragment);
    lastMessagesJSON = messagesJSON;

    // Keep the latest message visible when appropriate.
    if (wasNearBottom) {
        messageList.scrollTop = messageList.scrollHeight;
    }
}

// 9. GET /messages
async function fetchMessages() {
    // Do not start another poll if one is already running.
    if (isPolling) return;

    isPolling = true;

    try {
        const response = await fetch(API.messages, {
            method: "GET",
            cache: "no-store"
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        const messages = await response.json();

        if (!Array.isArray(messages)) {
            throw new Error("Expected a JSON array of messages.");
        }

        renderMessages(messages);
        updateConnectionStatus(true);
    } catch (error) {
        console.error("Failed to fetch messages:", error);
        updateConnectionStatus(false);
    } finally {
        isPolling = false;
    }
}

// 10. POST /send
async function sendMessage(event) {
    event.preventDefault();

    if (isSending) return;

    const username = usernameInput.value.trim();
    const content = contentInput.value.trim();

    if (!username || !content) {
        formStatus.textContent =
            "Please enter a username and a message.";
        return;
    }

    isSending = true;
    sendButton.disabled = true;
    sendLabel.textContent = "Sending...";
    formStatus.textContent = "Sending your message...";

    try {
        const body = new URLSearchParams({
            username: username,
            content: content
        });

        const response = await fetch(API.send, {
            method: "POST",
            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: body
        });

        if (!response.ok) {
            throw new Error(`HTTP error: ${response.status}`);
        }

        // Clear the input only after the server accepts the request.
        contentInput.value = "";
        formStatus.textContent = "Message sent successfully.";

        // Fetch immediately instead of waiting for the next poll.
        await fetchMessages();

        contentInput.focus();
    } catch (error) {
        console.error("Failed to send message:", error);
        formStatus.textContent =
            "Could not send message. Please try again.";
    } finally {
        isSending = false;
        sendButton.disabled = false;
        sendLabel.textContent = "Send message";
    }
}

// 11. Connect the form submission to sendMessage().
messageForm.addEventListener("submit", sendMessage);

// 12. Enter sends; Shift + Enter creates a new line.
contentInput.addEventListener("keydown", (event) => {
    if (
        event.key === "Enter" &&
        !event.shiftKey &&
        !event.isComposing
    ) {
        event.preventDefault();
        messageForm.requestSubmit();
    }
});

// 13. Load messages immediately, then poll every two seconds.
fetchMessages();
setInterval(fetchMessages, POLL_INTERVAL);
