# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

F2CG is a real-time 2-player collectible card game (CCG) built as a portfolio project. Backend is Spring WebFlux (reactive/non-blocking), frontend is React + Zustand + TypeScript. Both players see game state updates simultaneously via Server-Sent Events (SSE).

## Commands

```bash
# Backend
cd backend
mvn test              # Run all tests
mvn spring-boot:run   # Run backend (port 8080)

# Full stack
docker compose up     # Frontend at :3000, backend at :8080
```

H2 console available at `http://localhost:8080/h2-console` (user: `sa`, no password) during development.

## Architecture

Clean Architecture with four layers:

- **`domain/`** — Pure Java records and sealed interfaces; no framework dependencies
  - `card/` — `Card` sealed interface with `UnitCard`, `BuffCard`, `DebuffCard`
  - `game/` — `GameState`, `GamePhase` (FSM enum), `FieldUnit`, `SummoningState`
  - `player/` — `PlayerState` (hand, field, graveyard, deck size)
- **`application/`** — Business logic (`GameService` runs the turn FSM, `DeckService` manages decks)
- **`api/`** — Spring WebFlux controllers: `GameController` (REST for player actions), `SseController` (SSE stream endpoint)
- **`infrastructure/`** — Framework glue: `sse/` (SSE broadcast), `r2dbc/` (reactive DB access)

**Request flow:** REST action → `GameController` → `GameService` (state transition) → persist via R2DBC → emit new `GameState` via SSE → both browsers re-render.

**Game turn FSM:** `SUMMONING` (simultaneous secret placement + reveal) → `ACTION` (alternating attacks/abilities) → `DRAW` → `CHECK_VICTORY` → repeat.

## Key Design Decisions

- Domain objects are **immutable Java records** — always produce new state, never mutate.
- `Card` is a **sealed interface** — use pattern matching (`instanceof`/`switch`) exhaustively when handling card types.
- All I/O is **reactive** (`Mono`/`Flux`): Spring WebFlux for HTTP, R2DBC for database. Avoid blocking calls.
- SSE is used instead of WebSockets (unidirectional push is sufficient for this game).
- H2 in-memory DB for zero-config development; schema auto-initialized via Spring SQL init.