# grid07-virality-engine

A robust, high-performance Spring Boot microservice that acts as a central API gateway and guardrail system. Built for the **Grid07 Backend Engineering Assignment**.

---

## Tech Stack

- **Java 17+**
- **Spring Boot 3.x**
- **PostgreSQL 15** — Source of truth for all content
- **Redis 7** — Gatekeeper for all guardrails, counters, and notifications
- **Docker & Docker Compose** — Local infrastructure setup
- **Lombok** — Boilerplate reduction

---

## Features

### Phase 1 — Core API & Database
- JPA/Hibernate entities: `User`, `Bot`, `Post`, `Comment`
- REST endpoints:
  - `POST /api/posts` — Create a new post
  - `POST /api/posts/{postId}/like` — Like a post (+20 virality)
  - `POST /api/posts/{postId}/comments` — Add a comment with full guardrail checks

### Phase 2 — Redis Virality Engine & Atomic Locks
- **Virality Score** — Real-time scoring stored in Redis
  - Bot Reply = +1 point
  - Human Like = +20 points
  - Human Comment = +50 points
- **Horizontal Cap** — Max 100 bot replies per post (`post:{id}:bot_count`)
- **Vertical Cap** — Max 20 depth levels per comment thread
- **Cooldown Cap** — Bot cannot interact with the same human more than once per 10 minutes

### Phase 3 — Smart Notification Engine
- If user has been notified in last 15 minutes → push to Redis List (`user:{id}:pending_notifs`)
- If not → send immediately and set 15-minute cooldown
- `@Scheduled` CRON sweeper runs every 5 minutes, batches pending notifications and logs a summarized message

### Phase 4 — Concurrency & Race Condition Safety
- 200 concurrent bot requests tested — horizontal cap stops at **exactly 100**
- Achieved using **Redis Lua scripts** for atomic INCR + check operations
- Fully stateless — no Java `HashMap` or static variables used anywhere

---

## How Thread Safety is Guaranteed (Atomic Locks)

The biggest challenge in this assignment was ensuring that exactly 100 bot comments are allowed on a post even under 200 concurrent requests.

### The Problem
A naive approach using `INCR` + `if count > 100` in Java is **not atomic**. Two threads can both increment the counter to 100 and both pass the check before either gets rejected — resulting in 101 or more comments slipping through.

### The Solution — Lua Script
Redis executes Lua scripts **atomically**. No other command can run between the `INCR` and the check. Here's the script used:

```lua
local current = redis.call('INCR', KEYS[1])
if current > tonumber(ARGV[1]) then
  redis.call('DECR', KEYS[1])
  return -1
else
  return current
end
```

This guarantees that the increment and the boundary check happen as a single atomic operation — making it impossible for more than 100 bot comments to be saved even under extreme concurrency.

---

## Project Structure

```
src/main/java/com/grid07/grid07/
├── entity/
│   ├── User.java
│   ├── Bot.java
│   ├── Post.java
│   └── Comment.java
├── repository/
│   ├── UserRepository.java
│   ├── BotRepository.java
│   ├── PostRepository.java
│   └── CommentRepository.java
├── service/
│   └── PostService.java
├── controller/
│   └── PostController.java
└── scheduler/
    └── NotificationScheduler.java
```

---

## Getting Started

### Prerequisites
- Docker Desktop installed and running
- Java 17+
- Maven

### 1. Clone the repository
```bash
git clone https://github.com/AmanAnsary23/grid07-virality-engine.git
cd grid07-virality-engine
```

### 2. Start PostgreSQL and Redis
```bash
docker compose up -d
```

### 3. Run the application
```bash
./mvnw spring-boot:run
```

The server starts at `http://localhost:8080`

---

## API Endpoints

### Create a Post
```
POST /api/posts
Content-Type: application/json

{
    "authorId": "1",
    "authorType": "USER",
    "content": "Hello this is my first post!"
}
```

### Like a Post
```
POST /api/posts/1/like
```

### Add a Comment (Bot)
```
POST /api/posts/1/comments
Content-Type: application/json

{
    "authorId": "1",
    "authorType": "BOT",
    "content": "Nice post!",
    "depthLevel": "1",
    "botId": "1",
    "humanId": "1"
}
```

---

## Guardrail Responses

| Scenario | HTTP Status | Message |
|---|---|---|
| Bot comment limit exceeded (>100) | 429 | REJECTED: Bot reply limit reached for this post (max 100) |
| Comment too deep (>20 levels) | 429 | REJECTED: Comment thread too deep (max 20 levels) |
| Bot on cooldown (within 10 min) | 429 | REJECTED: Bot is in cooldown period for this user (10 min) |

---

## Race Condition Test Results

Fired 200 concurrent bot requests on a single post:

```
Firing 200 concurrent bot requests...
✅ Success: 100
❌ Rejected: 100
🎉 RACE CONDITION TEST PASSED!
```

---

## Environment Variables (application.properties)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/grid07db
spring.datasource.username=grid07user
spring.datasource.password=grid07pass
spring.jpa.hibernate.ddl-auto=update
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

## Author

**Aman Ansari**  
B.Tech CSE | Backend Developer  
[GitHub](https://github.com/AmanAnsary23)
