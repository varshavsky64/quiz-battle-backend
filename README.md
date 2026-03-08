# Quiz Battle Backend

A real-time multiplayer quiz game backend built with Spring Boot 4 and WebSocket (STOMP). Players compete in 5-round matches answering multiple-choice questions against a random opponent or a bot. The service is designed to be consumed by an iOS client, with a built-in web UI for testing and question management.

## How It Works

A match consists of 5 rounds. Each round presents one question with 4 answer options. Players answer independently — there is no waiting for the opponent between rounds. After answering (or after the 10-second timeout expires), the correct answer is revealed immediately. Once a player finishes all 5 rounds, they see the results; if the opponent hasn't finished yet, the results update automatically via WebSocket when they do. The winner is the player who answered more rounds correctly. A draw is possible.

**Matchmaking options:**
- **Random queue** — join a queue and get matched with another waiting player. If no opponent is found within 10 seconds, a bot is created.
- **Private room** — create a room and share the 6-character code with a friend to start a match together.

**Bot behavior:** when matched against a bot, its answers are pre-computed immediately at match creation. The bot answers correctly ~50% of the time and simulates a realistic finish time (random per-round delays of 2–9 seconds).

## Tech Stack

- **Java 21**, Spring Boot 4.0
- **WebSocket** (STOMP over `/ws`) for real-time game events
- **Spring WebMVC** for REST API
- **Spring Data JPA** + **PostgreSQL** for persistence
- **Flyway** for database migrations
- **Lombok** for boilerplate reduction

## Prerequisites

- Java 21+
- PostgreSQL running on `localhost:5432`
- A database named `quizbattle` with user `postgres` / password `password`

```sql
CREATE DATABASE quizbattle;
```

Credentials can be changed in `src/main/resources/application.yaml`.

## Running

**With Docker (recommended):**

```bash
docker compose up --build
```

This starts PostgreSQL and the application together. The database is created automatically, and Flyway applies the schema on first boot. Data is persisted in a named Docker volume (`postgres_data`).

**Locally (requires PostgreSQL on `localhost:5432`):**

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`. Flyway applies the schema automatically on first run.

## Running Tests

```bash
./mvnw test
```

Tests use Mockito with a Spring test context (`@WebMvcTest` for controllers, `@Import` + `@MockitoBean` for services). No database required.

## Architecture

```
src/main/java/.../quizbattle/
├── config/          AppProperties, WebSocketConfig, SchedulerConfig
├── controller/      PlayerController, MatchController, MatchWebSocketController
│   └── admin/       AdminQuestionController
├── dto/
│   ├── request/     Incoming REST request bodies
│   ├── response/    REST response bodies
│   └── ws/          WebSocket message payloads
├── entity/          JPA entities (Player, Question, Answer, Match, MatchRound, ...)
├── interceptor/     PlayerSessionInterceptor — binds STOMP session to playerId
├── match/           In-memory match state (MatchSession, PlayerRoundState, RoundResult)
├── repository/      Spring Data JPA repositories
└── service/         PlayerService, QuestionService, MatchmakingService,
                     MatchService, BotService
```

**Match state** is kept in memory (`ConcurrentHashMap`) during a match and persisted to the database once both players finish.

**Round timer** is server-side: each round start schedules a 10-second `TaskScheduler` task. If the player doesn't answer in time, a null answer (incorrect) is recorded automatically.

**Question selection** avoids recently seen questions per player. The last 20 questions a player has answered are excluded from the pool; if fewer than 5 remain, the pool is filled from random questions.

## REST API

All public endpoints are versioned under `/api/v1`.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/players` | Create a player (body: `{ "name": "Alice" }`) |
| `GET` | `/api/v1/players/{id}` | Get player info (wins, losses) |
| `POST` | `/api/v1/matches/random?playerId=` | Join the random matchmaking queue |
| `POST` | `/api/v1/matches/private?playerId=` | Create a private room, returns a 6-char code |
| `POST` | `/api/v1/matches/private/{code}/join?playerId=` | Join a private room by code |
| `GET` | `/api/v1/matches/{id}` | Get match result |
| `GET` | `/api/v1/matches/player/{playerId}?page=0&size=10` | Paginated match history |

## WebSocket

Connect to `ws://localhost:8080/ws` using STOMP. Send `playerId` as a header in the CONNECT frame, then subscribe to `/topic/player/{playerId}`.

**Server → client messages** (all wrapped as `{ "type": "...", "payload": { ... } }`):

| Type | When | Key payload fields |
|------|------|--------------------|
| `MATCH_FOUND` | Match created | `matchId`, `opponentName`, `bot` |
| `ROUND_START` | New round ready | `roundNumber`, `totalRounds`, `questionText`, `answers[]`, `timeoutSeconds` |
| `ROUND_END` | Answer submitted or timed out | `roundNumber`, `correctAnswerId`, `yourAnswerId`, `correct` |
| `MATCH_END` | Player finished all rounds | `rounds[]`, `yourScore`, `opponentScore`, `winnerId`, `opponentFinished` |
| `MATCH_UPDATED` | Opponent finished (sent after `MATCH_END`) | `opponentRounds[]`, `opponentScore`, `winnerId` |

**Client → server** (STOMP destinations):

| Destination | Body | Description |
|-------------|------|-------------|
| `/app/match/answer` | `{ "matchId", "roundNumber", "answerId" }` | Submit an answer (`answerId` can be null for timeout) |
| `/app/match/next` | `{ "matchId" }` | Request the next round after seeing the result |

## Admin Panel

The admin panel is a standalone web page for managing the question bank. It is served at:

```
http://localhost:8080/admin.html
```

It is intentionally not versioned or protected (basic auth can be added later) and is meant to be used by the team directly on the server.

### What you can do

- **View all questions** — paginated table showing question text, difficulty, and answers with the correct one highlighted.
- **Add a question** — fill in the question text, difficulty (1–3), and exactly 4 answer options. Mark exactly one as correct.
- **Edit a question** — click Edit on any row to load it into the form. Saving replaces all answers.
- **Delete a question** — removes the question and all its answers permanently.

### Admin API

The admin endpoints are under `/admin/questions` (no version prefix):

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/questions?page=0&size=20` | List questions (paginated) |
| `POST` | `/admin/questions` | Create a question |
| `PUT` | `/admin/questions/{id}` | Replace a question |
| `DELETE` | `/admin/questions/{id}` | Delete a question |

**Minimum question payload:**
```json
{
  "text": "What is the capital of France?",
  "difficulty": 1,
  "answers": [
    { "text": "Paris",  "correct": true,  "position": 0 },
    { "text": "London", "correct": false, "position": 1 },
    { "text": "Berlin", "correct": false, "position": 2 },
    { "text": "Madrid", "correct": false, "position": 3 }
  ]
}
```

Exactly one answer must have `"correct": true`. The `position` field controls display order. Difficulty is an integer from 1 to 3.

## Game Test Client

A simple browser-based game client for testing the full game flow is available at:

```
http://localhost:8080/game.html
```

It covers the full player journey: registration by name, matchmaking, live rounds with countdown timer, answer reveal, and match results including the "opponent still playing" state.

## Configuration

Key settings in `application.yaml` under the `app` prefix:

| Property | Default | Description |
|----------|---------|-------------|
| `app.matchmaking.queue-timeout-seconds` | `10` | Seconds to wait in random queue before creating a bot match |
| `app.matchmaking.rounds-per-match` | `5` | Number of rounds per match |
| `app.matchmaking.round-timeout-seconds` | `10` | Seconds per round before auto-submitting a null answer |
| `app.bot.correct-answer-probability` | `0.5` | Probability the bot answers correctly |
| `app.bot.min-answer-delay-seconds` | `2` | Minimum simulated per-round delay for the bot |
| `app.bot.max-answer-delay-seconds` | `9` | Maximum simulated per-round delay for the bot |
