# Pocket Dozzle - Mobile Log Viewer Backend

MVP Backend for streaming Docker container logs to iOS clients.

## 🚀 Features
- **REST API**: List all running containers.
- **WebSocket**: Real-time log streaming for a specific container.
- **Production-ready**: Handles client disconnects and resource cleanup.

## 🛠 Tech Stack
- **Java 21**
- **Spring Boot 4.0.2**
- **Docker Java Client** (com.github.docker-java)
- **WebSockets**

## 🔌 API Contract

### REST API
**`GET /containers`**

Returns a list of all containers (running and stopped).

**Response:**
```json
[
  {
    "id": "7648f574a3f1...",
    "name": "web-server",
    "status": "running"
  }
]
```

---

### WebSocket API
**`WS /logs?containerId={containerId}`**

Streams logs for the specified container.

**Stream Message:**
```json
{
  "timestamp": "2026-01-25T12:30:01Z",
  "line": "Application started on port 8080"
}
```

## ⚙️ Configuration

The application connects to the Docker Engine using the default host.
- **Linux/macOS**: `unix:///var/run/docker.sock`
- **Windows**: `npipe:////./pipe/docker_engine`

Ensure the backend has permissions to access the Docker socket.

## 🏃 How to Run

### Prerequisites
- Docker installed and running.
- Java 21 installed.
- Maven installed (or use `./mvnw`).

### Steps
1. **Clone the repository.**
2. **Build the application:**
   ```bash
   ./mvnw clean package
   ```
3. **Run the application:**
   ```bash
   java -jar target/dozzle-mobile-app-0.0.1-SNAPSHOT.jar
   ```
   Or using Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

The backend will start on `http://localhost:8080`.

## 🏗 Infrastructure Details

- **`DockerConfig`**: Configures the `DockerClient` with `ApacheDockerHttpClient` for reliable communication with the Docker Engine.
- **`LogWebSocketHandler`**: Uses the `docker-java` streaming API. It attaches a `ResultCallback` to the Docker log stream and forwards frames to the WebSocket session.
- **Resource Management**: The `watchRequests` map tracks active Docker log streams per WebSocket session. When a client disconnects, the Docker stream is explicitly closed to prevent memory leaks and orphaned processes.
- **Non-blocking**: Logs are streamed asynchronously as they are produced by the container.
