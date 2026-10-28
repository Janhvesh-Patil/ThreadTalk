# ThreadTalk

A multi-client chat room built from scratch in Java — no frameworks, no libraries beyond JDBC. Multiple browser tabs connect to a raw Java `ServerSocket` via HTTP long-polling. Messages persist in SQLite across server restarts.

Built as a deliberate learning exercise to understand concurrency before Spring Boot.

---

## What it does

- Multiple browser clients connect simultaneously over HTTP
- Each client is handled by its own thread from a fixed thread pool
- Messages are stored in a thread-safe in-memory list and persisted to SQLite
- The frontend polls for new messages every 2 seconds
- Chat history survives server restarts

---

## What it teaches

This project exists to make concurrency tangible before a framework hides it.

| Concept | Where it appears |
|---|---|
| Thread-per-client model | `Server.java` spawns one `ClientHandler` thread per connection |
| Race conditions | `MessageStore` — shared mutable state across all threads |
| `synchronized` | Every method on `MessageStore` and `Database` that touches shared state |
| Producer-consumer pattern | `addMessage()` writes, `getAllMessages()` reads — serialized via object monitor |
| JDBC with SQLite | Single shared connection, synchronized writes, schema init on startup |
| Raw HTTP parsing | Manual request line, header, and body parsing without any web framework |

Spring Boot's `@Transactional`, its thread-safe request handling, and its connection pooling all exist because of the problems this project makes visible.

---

## Tech stack

- **Java 25** — raw `ServerSocket`, `ExecutorService`, `synchronized`
- **SQLite via JDBC** — `org.xerial:sqlite-jdbc:3.53.2.0`
- **HTML / CSS / JavaScript** — vanilla, no frontend framework
- **Tailwind CSS** — via CDN for styling only

---

## Project structure

```
ThreadTalk/
├── src/threadtalk/
│   ├── Server.java          # ServerSocket accept loop, thread pool, startup
│   ├── ClientHandler.java   # Runnable — parses HTTP, routes requests, writes response
│   ├── MessageStore.java    # Thread-safe in-memory message list
│   ├── Database.java        # JDBC/SQLite — schema, insert, select
│   ├── HttpUtils.java       # Raw HTTP parser and response builder
│   └── Message.java         # Immutable message POJO
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── script.js
├── lib/
│   └── sqlite-jdbc-3.53.2.0.jar
└── data/
    └── chat.db              # created at runtime, gitignored
```

---

## How to run

**Requirements:** Java 17+

**1. Clone the repo**
```bash
git clone https://github.com/Janhvesh-Patil/ThreadTalk.git
cd ThreadTalk
```

**2. Download the SQLite JDBC driver**

Download `sqlite-jdbc-3.53.2.0.jar` from [Maven Central](https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.53.2.0/sqlite-jdbc-3.53.2.0.jar) and place it in the `lib/` folder.

**3. Run via IntelliJ**

Open the project in IntelliJ IDEA, mark `src` as Sources Root, add `lib/sqlite-jdbc-3.53.2.0.jar` as a library, then run `Server.java`.

**4. Open the app**

Navigate to `http://localhost:8081` in your browser. Open multiple tabs to test concurrent messaging.

---

## Concurrency design

`MessageStore` is the single shared object that all client threads access simultaneously. Without synchronization, two threads calling `addMessage()` at the same moment can both read the same `lastId`, producing duplicate message IDs and corrupting the list.

Every public method on `MessageStore` is marked `synchronized`, which acquires the object's intrinsic lock before executing. This serializes concurrent access — Thread B waits at the method entry until Thread A exits. `synchronized` also guarantees memory visibility: Thread B always reads Thread A's latest write to `lastId`, not a stale CPU-cached value.

`Database` uses the same pattern. SQLite permits only one writer at a time; synchronized methods on a shared `Connection` instance enforce that constraint at the Java layer before it ever reaches SQLite.

---

## Known limitations

- HTTP long-polling, not WebSockets — the frontend polls every 2 seconds rather than receiving push updates
- Single chat room — no named rooms or user authentication
- The SQLite jar must be downloaded manually — no build tool dependency management

These are intentional. The goal was to understand the primitives, not to build a production system.