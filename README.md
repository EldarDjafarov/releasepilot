# ReleasePilot

Internal platform that moves application versions through deployment environments: `DEV → STAGING → PRODUCTION`.

## Quick Start

```bash
# Start PostgreSQL and RabbitMQ
docker-compose up postgres rabbitmq -d

# Run the application
./mvnw spring-boot:run

# Run all tests (Testcontainers spins up its own containers)
./mvnw test
```

Or start everything including the app:
```bash
docker-compose up -d
```

RabbitMQ management UI: http://localhost:15672 (guest/guest)

---

## Architecture

### Package Structure

```
com.releasepilot
├── domain/                     # Pure domain — no Spring, no I/O
│   ├── model/                  # Promotion aggregate, Environment, PromotionStatus
│   ├── event/                  # Domain events (sealed hierarchy)
│   ├── exception/              # Domain rule violations (never 500s)
│   ├── port/                   # Interfaces the domain needs (dependency inversion)
│   └── service/                # ApproverService domain service
│
├── application/                # Orchestration — thin, no business logic
│   ├── command/                # PromotionCommand sealed interface + handlers
│   └── query/                  # Read models + query handlers
│
└── infrastructure/             # All I/O and frameworks
    ├── persistence/            # JPA entities, repositories, repository adapter
    ├── messaging/              # RabbitMQ publisher, AuditLogConsumer, ReleaseNotesConsumer
    ├── adapter/                # In-memory stubs for external ports
    ├── agent/                  # Release notes AI agent + mock LLM backend
    └── web/                    # REST controllers, GlobalExceptionHandler
```

### Why ports live in the domain layer

`DeploymentPort`, `IssueTrackerPort`, `NotificationPort` are defined in `domain/port/`, not in `infrastructure/`. This is the **Dependency Inversion Principle** applied strictly:

- The domain defines *what capability it needs* (the interface)
- Infrastructure provides *how it's done* (the implementation)
- The dependency arrow points **inward**: infrastructure depends on domain, never the reverse

Result: you can test the entire domain + application layer with zero Spring context, swapping any port for a test double.

### Promotion aggregate

The aggregate is pure Java — no JPA annotations, no Spring annotations. It:
- Holds all business rules
- Collects domain events internally (`domainEvents()` list)
- Exposes a `reconstitute()` factory for rebuilding from persisted state without re-triggering events

Events are published **after** a successful `repository.save()`. This avoids publishing events for rolled-back transactions. The tradeoff: if publishing fails after persist, the event is lost. A proper **outbox pattern** would eliminate this — noted as a future improvement.

### CQRS split

Commands go through the aggregate. Queries bypass it entirely and read directly from JPA entities. This is intentional:

- No point loading a full aggregate to return a list of summaries
- Read models (`PromotionReadModels`) are shaped for what the API returns, not for what the aggregate needs
- The `PromotionQueryHandlers` can evolve independently — add a materialized view, a read replica, or a denormalized table without touching the aggregate

### Event store design

Events are stored in `promotion_events` with typed columns, not JSONB. Rationale:

- There are exactly 6 event types with predictable fields
- Structured columns are trivially queryable and indexable
- Migrations on typed columns are straightforward
- JSONB is the right choice when the schema is truly unknown — that's not the case here

### RabbitMQ topology

```
Exchange: promotion.events (topic)
    ├── routing key "#"                 → audit.queue         (ALL events)
    └── routing key "PromotionApproved" → release-notes.queue (agent trigger)
```

Topic exchange means new consumers can subscribe to any subset of events purely through configuration, without changing the publisher.

### AI Release Notes Agent

A hand-rolled tool-calling loop in `ReleaseNotesAgent`. The structure:

1. Agent gets a goal + list of tool definitions
2. LLM (mocked) decides which tool to call
3. Tool executor runs the tool and returns a result
4. Result is appended to the message history
5. Loop repeats until `SubmitReleaseNotes` is called (terminal)

To wire in a real LLM: replace `MockLlmBackend` with an Anthropic/OpenAI client that serializes the `messages` list + `ToolDefinition` list to the API format and parses `tool_use` blocks from the response. Nothing else changes.

---

## API Reference

### Commands

| Method | Path | Description |
|--------|------|-------------|
| POST | `/promotions` | Request a new promotion |
| POST | `/promotions/{id}/approve` | Approve (approver role required) |
| POST | `/promotions/{id}/start-deployment` | Start the deployment |
| POST | `/promotions/{id}/complete` | Mark deployment successful |
| POST | `/promotions/{id}/rollback` | Roll back (from IN_PROGRESS only) |
| POST | `/promotions/{id}/cancel` | Cancel (from PENDING or APPROVED) |

### Queries

| Method | Path | Description |
|--------|------|-------------|
| GET | `/promotions/{id}` | Full detail + state history |
| GET | `/applications/{id}/status` | Current status per environment |
| GET | `/applications/{id}/promotions` | Paged promotion history |

### Error responses (RFC 9457 ProblemDetail)

| Exception | HTTP Status | type |
|-----------|-------------|------|
| EnvironmentSkippedException | 422 | `environment-skipped` |
| PromotionAlreadyInProgressException | 409 | `promotion-already-in-progress` |
| InvalidPromotionStateException | 422 | `invalid-promotion-state` |
| UnauthorizedApprovalException | 403 | `unauthorized-approval` |
| PromotionNotFoundException | 404 | `promotion-not-found` |

---

## What's Missing / Future Work

- **Authentication**: `requestedBy` / `actingUser` are passed in the request body. In production these come from a JWT claim.
- **Approver RBAC**: `ApproverService` uses a hardcoded set. Real implementation: Spring Security + DB/LDAP.
- **Real LLM backend**: `MockLlmBackend` → Anthropic or OpenAI client with tool_use parsing.
- **Webhook for deployment completion**: currently `CompletePromotion` is called manually. A real system would receive a callback from the deployment platform.
