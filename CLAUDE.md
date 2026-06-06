# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Remedify** is a Code Review Pipeline Automation tool that scans GitHub repositories for security vulnerabilities, uses Claude AI to recommend high-severity fixes, validates builds/tests, and generates comprehensive reports. The application is designed for a single technical user (Asutosh) to locally audit public Java/Spring Boot repositories.

**Design Reference**: The UI implements the [Linear design system](./design.md) — dark canvas (#010102), lavender accent (#5e6ad2), minimal elevation hierarchy.

## Architecture

Remedify follows a **client-server** architecture:

### Backend (Spring Boot 3.x)
- **Orchestration Layer**: `ScanOrchestrationService` coordinates the 5-stage pipeline sequentially
- **Service Layer**: Separate services for each scanning concern (clone, detect, recommend, validate, report)
- **Integration Layer**: External service adapters (GitHub, Snyk, OWASP, Claude API via Spring AI)
- **Persistence**: Spring Data JPA with H2 in-memory + SQLite file-based storage
- **Real-time Updates**: Server-Sent Events (SSE) for frontend card state changes

### Frontend (React + Vite)
- **Kanban Board**: Interactive pipeline visualization with 5 columns (Cloning → Scanning → Recommending → Validating → Reporting → Completed)
- **Repository Cards**: Draggable cards showing repo name, current stage, status badge, vulnerability count
- **SSE Listener**: `useScanProgress` hook listens to `/api/scans/:id/sse` for real-time updates
- **Linear Design**: TailwindCSS configured with Linear palette (colors, typography, spacing tokens)

### Data Model
- **RepositoryScan**: Tracks a single scan's lifecycle (URL, stage, retry count, temp paths)
- **Vulnerability**: Individual findings (severity, type, file location, CVE ID)
- **AIRecommendation**: Claude API outputs (fix suggestions, effort estimates)
- **TestResult**: Maven build/test outputs (pass/fail, logs)

## Token Efficiency Strategy

The Claude API integration is **intentionally conservative**:
- Only processes **Critical + High severity** vulnerabilities (saves ~80% of tokens vs. all issues)
- **Batches similar issues** by type to leverage prompt caching (e.g., all log4j findings in one call)
- **Caches system messages** to avoid re-analyzing identical patterns
- **Max 1024 tokens per request** to cap cost per scan (~$0.50–$1.00 per scan, ~$200/year at 1 scan/day)

Code that calls Claude (`ClaudeAIIntegration.java`) must:
1. Filter: `severity in [CRITICAL, HIGH]` before building prompts
2. Group: Collect vulnerabilities by type
3. Batch: Send 5+ similar issues in one prompt, not one-by-one
4. Cache: Reuse the same system message for all requests

## Common Development Commands

### Backend (Spring Boot)
```bash
# Build (Maven)
mvn clean package

# Run locally
mvn spring-boot:run

# Run a single test class
mvn test -Dtest=RepositoryScanRepositoryTest

# Run tests in the integration/ folder
mvn test -Dtest=*Integration*

# Check for dependency vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Format code (if configured)
mvn spotless:apply
```

### Frontend (React)
```bash
cd frontend/

# Install dependencies
npm install

# Dev server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Run unit tests (Jest)
npm test

# Format code
npm run format
```

### Full Stack (Local Development)
```bash
# Terminal 1: Start backend
mvn spring-boot:run

# Terminal 2: Start frontend
cd frontend && npm run dev

# App is now at http://localhost:5173
# Backend API is at http://localhost:8080/api
```

## Directory Structure

```
remedify/
├── src/main/java/com/remedify/
│   ├── controller/          # REST endpoints
│   ├── service/             # Business logic (orchestration, scanning, AI)
│   ├── model/               # JPA entities (RepositoryScan, Vulnerability, etc.)
│   ├── integration/         # External service adapters (GitHub, Snyk, OWASP, Claude)
│   ├── repository/          # Spring Data JPA interfaces
│   └── config/              # Spring configuration (Spring AI, async task executor)
├── src/main/resources/
│   └── application.yml      # Properties: API keys, Claude model, logging
├── src/test/java/           # Unit & integration tests
├── frontend/
│   ├── src/components/      # React components (KanbanBoard, RepositoryCard, etc.)
│   ├── src/hooks/           # Custom hooks (useScanProgress, useRepository)
│   ├── src/services/        # API client (Axios wrapper)
│   ├── src/styles/          # TailwindCSS config + Linear design tokens
│   └── package.json
├── pom.xml                  # Maven dependencies
└── design.md                # Linear design system specification
```

## Key Implementation Details

### Pipeline Execution (Synchronous, Sequential)
1. User POSTs GitHub URL to `/api/scans` → card created in "Cloning" column
2. Backend spawns async task for each stage in sequence (no parallelization)
3. Each stage completes, updates `RepositoryScan.currentStage` in DB
4. SSE emits event to frontend: `{ stage: "SCANNING", statusMessage: "..." }`
5. Frontend updates card position via Kanban state manager
6. **Error Handling**: If stage fails → auto-retry after 30s (max 2 retries), then alert user with error badge

### Database Retention
- **Storage**: H2 in-memory for dev; SQLite file at `~/.remedify/scans.db` for persistence
- **Retention**: Keep last 20 completed scans; auto-delete older ones on startup
- **Temp Files**: Clone repos to `/tmp/remedify-{scanId}`, cleanup on completion or 24h TTL

### Configuration (application.yml)
```yaml
# Must be set by user before first run:
remedify:
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}  # Claude API key
    model: claude-3-5-sonnet-20241022
    max-tokens: 1024
    enable-cache: true  # Prompt caching for cost savings
  snyk:
    api-key: ${SNYK_API_KEY}  # Optional; OWASP alone will work
    enabled: true
  github:
    base-url: https://github.com
    clone-timeout-seconds: 300  # 5 min timeout
  database:
    path: ~/.remedify/scans.db
    retention-days: 30
    max-scans-to-keep: 20
```

## Testing Strategy

### Unit Tests
- Service layer: Mock external APIs (GitHub, Snyk, Claude)
- Repository layer: H2 in-memory database for quick feedback
- Components: React Testing Library + Mock SSE events

### Integration Tests
- Full pipeline: Mock GitHub (provide pre-cloned fixture), mock Snyk API, mock Claude
- Verify stage transitions and DB persistence
- Verify report generation format (JSON/HTML)

### Manual Testing
1. Run backend + frontend locally
2. Add a public repo (e.g., `https://github.com/spring-projects/spring-boot`)
3. Watch card move through stages in real-time
4. Verify vulnerabilities appear (OWASP findings)
5. Verify AI recommendations show only for high-severity issues
6. Verify report downloads correctly

## Notable Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Sequential, not parallel stages** | Easier to debug, respect resource limits (don't clone + scan + validate all at once) |
| **No user authentication** | Personal tool, runs locally only |
| **Public repos only** | Simpler; no GitHub token setup needed |
| **H2 + SQLite, not PostgreSQL** | Zero-config local dev; no external DB needed |
| **SSE for real-time, not WebSockets** | Simpler protocol, sufficient for single user |
| **OWASP + Snyk, not custom scanner** | Existing tools are battle-tested; integration work only |
| **High-severity-only AI** | Token cost management; low-value to analyze every medium/low issue |

## Codebase Conventions

- **Package structure**: Follows Spring Boot standard (controller → service → model → repository)
- **Naming**: CamelCase for classes, kebab-case for file names where applicable, UPPER_SNAKE for constants
- **Async tasks**: Marked with `@Async` in service layer; TaskExecutor configured in `AsyncConfig`
- **Error handling**: Custom `ScanException` for domain logic; let Spring handle HTTP status codes
- **Logging**: SLF4J via Spring; ERROR level for failures, INFO for stage transitions, DEBUG for API calls
- **Comments**: Minimal; prefer clear naming. Add comments only for non-obvious retry logic or API quirks

## Future Considerations

- **Multi-user support**: Would require adding user authentication (Spring Security) and RBAC
- **Parallel scanning**: Could parallelize stages 2–4 if resource constraints ease
- **Caching layer**: Could add Redis for frequently-scanned repos
- **Report formats**: Currently HTML/JSON; could add PDF export, custom templates
- **CI/CD integration**: Could expose API for GitHub Actions, GitLab CI pipelines

## References

- **Spring Boot 3.x Docs**: https://spring.io/projects/spring-boot
- **Spring AI Docs**: https://docs.spring.io/spring-ai/
- **Anthropic API Docs**: https://docs.anthropic.com
- **React Docs**: https://react.dev
- **TailwindCSS Docs**: https://tailwindcss.com
- **Plan File**: See `/claude/plans/merry-yawning-clover.md` for detailed architecture breakdown
