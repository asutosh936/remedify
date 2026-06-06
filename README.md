# Remedify

**AI Software Modernization Platform** — Code Review Pipeline Automation with AI-Powered Vulnerability Fixes

Remedify is a web application that automates code security scanning by reading GitHub repositories, detecting vulnerabilities, generating AI-powered fix recommendations, validating builds/tests, and producing comprehensive reports.

## 📋 Requirements

### Prerequisites
- **Java 17+** — For the Spring Boot backend
- **Maven 3.8+** — For building the Java backend
- **Node.js 18+** — For the React frontend
- **npm 9+** — For managing frontend dependencies

### Verify Installation
```bash
java -version          # Should show Java 17+
mvn -version          # Should show Maven 3.8+
node -version         # Should show Node.js 18+
npm -version          # Should show npm 9+
```

---

## 🚀 Getting Started

### Project Structure
```
remedify/
├── pom.xml                      # Backend: Maven configuration
├── src/main/java/com/remedify/  # Backend: Spring Boot code
├── src/main/resources/          # Backend: Configuration (application.yml)
├── frontend/                    # Frontend: React + Vite
│   ├── package.json
│   ├── src/
│   └── tsconfig.json
├── CLAUDE.md                    # Development guidance for Claude Code
└── README.md                    # This file
```

---

## 🔧 Backend Setup (Spring Boot 3.x)

### 1. Configure API Keys

Create a `.env` file or export environment variables for Claude API:

```bash
export ANTHROPIC_API_KEY="your-claude-api-key-here"
export SNYK_API_KEY="your-snyk-api-key-here"  # Optional
```

Or configure in `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    anthropic:
      api-key: your-api-key-here
```

### 2. Build Backend

```bash
# From the remedify root directory
mvn clean compile

# Or build a JAR for production
mvn clean package
```

### 3. Run Backend (Local Development)

**Terminal 1:**
```bash
# Start Spring Boot server on http://localhost:8080
mvn spring-boot:run
```

The backend will:
- Start on `http://localhost:8080`
- Create H2 in-memory database
- Expose REST API at `/api/scans`, `/api/reports`
- Enable Server-Sent Events (SSE) for real-time updates

**Verify Backend is Running:**
```bash
curl http://localhost:8080/api/scans
# Should return: {"content":[],"totalElements":0,...}
```

### 4. Backend Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
remedify:
  scanning:
    clone-timeout-seconds: 300        # 5 min timeout for git clone
    temp-dir: /tmp/remedify-scans     # Where to clone repos
    retention-days: 30                # Keep scan history 30 days
    max-scans-to-keep: 20             # Max 20 scans in database
  snyk:
    enabled: false                    # Set true if you have Snyk API key
    api-key: ${SNYK_API_KEY:}
  github:
    base-url: https://github.com
    clone-timeout-seconds: 300
```

### 5. Common Backend Commands

```bash
# Run tests
mvn test

# Run a specific test
mvn test -Dtest=RepositoryScanRepositoryTest

# Check for dependency vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Build production JAR
mvn clean package -DskipTests

# Run JAR directly
java -jar target/remedify-0.1.0.jar
```

---

## 🎨 Frontend Setup (React 18 + Vite)

### 1. Install Dependencies

**Terminal 2:**
```bash
# Navigate to frontend directory
cd frontend

# Install npm packages (one-time)
npm install
```

### 2. Run Frontend (Local Development)

```bash
# Still in the frontend/ directory
npm run dev
```

The frontend will:
- Start on `http://localhost:5173`
- Auto-reload on code changes
- Proxy API calls to `http://localhost:8080/api` (configured in `vite.config.ts`)

**Verify Frontend is Running:**
```bash
# In your browser
open http://localhost:5173
```

You should see the Remedify dashboard with the Kanban board (currently empty).

### 3. Frontend Configuration

Frontend automatically proxies API calls to the backend:
- `http://localhost:5173/api/*` → `http://localhost:8080/api/*`

To change backend URL, edit `frontend/vite.config.ts`:

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // ← Change this
      changeOrigin: true,
    }
  }
}
```

### 4. Common Frontend Commands

```bash
# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Type checking
npm run type-check

# Format code
npm run format

# Lint
npm run lint
```

---

## 🏃 Running Both Backend & Frontend

### Quick Start (Two Terminals)

**Terminal 1 — Backend:**
```bash
# From remedify root
mvn spring-boot:run
```

**Terminal 2 — Frontend:**
```bash
# From remedify root
cd frontend && npm run dev
```

Then open your browser to `http://localhost:5173` and start adding repositories!

### Using tmux (Single Terminal)

```bash
# Create new tmux session
tmux new-session -d -s remedify

# Split into two panes
tmux split-window -h

# Backend in left pane
tmux send-keys -t remedify:0.0 "mvn spring-boot:run" Enter

# Frontend in right pane
tmux send-keys -t remedify:0.1 "cd frontend && npm run dev" Enter

# View both
tmux attach -t remedify
```

---

## 📊 Logging Configuration

### Log Levels

The application uses SLF4J with detailed logging for debugging. Configure in `src/main/resources/application.yml`:

```yaml
logging:
  level:
    com.remedify: DEBUG                           # Application code
    org.springframework.web: INFO                 # HTTP requests
    org.hibernate.SQL: DEBUG                      # SQL queries
    org.hibernate.type.descriptor.sql: TRACE      # SQL parameters
```

### View Logs

**In Terminal (when running `mvn spring-boot:run`):**
```
2024-06-05 10:30:00.123 [main] INFO  c.remedify.RemedigyApplication - Started RemedigyApplication
2024-06-05 10:30:01.456 [remedify-async-1] DEBUG c.r.service.ScanOrchestrationService - Starting stage: CLONING for scan: 550e8400
2024-06-05 10:30:02.789 [remedify-async-1] DEBUG c.r.integration.GitHubIntegration - Cloning repository: https://github.com/spring-projects/spring-boot
```

**Log Levels (from most to least verbose):**
- `TRACE` — Very detailed diagnostic info (SQL parameter values)
- `DEBUG` — Detailed information for debugging
- `INFO` — General informational messages
- `WARN` — Warning messages (potential issues)
- `ERROR` — Error messages (failures)
- `FATAL` — Critical failures

### Enable File Logging (Optional)

Uncomment in `src/main/resources/application.yml`:

```yaml
logging:
  file:
    name: logs/remedify.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

Then logs will be written to `logs/remedify.log` with automatic rotation.

### H2 Console (Database Inspection)

During development, inspect the database at:

```
http://localhost:8080/h2-console
```

**Credentials:**
- JDBC URL: `jdbc:h2:mem:remedifydb`
- Username: `sa`
- Password: (leave empty)

---

## 🧪 Testing Setup

### Backend Tests
```bash
# Run all tests
mvn test

# Run specific test file
mvn test -Dtest=VulnerabilityRepositoryTest

# Run tests matching a pattern
mvn test -Dtest=*Integration*
```

### Frontend Tests (when implemented)
```bash
cd frontend
npm test
```

---

## 📡 API Endpoints & cURL Examples

### Base URL
```
http://localhost:8080/api
```

### 1. Create New Scan
**Endpoint:** `POST /scans`

**Description:** Create a new scan for a GitHub repository (public repos only)

**Request:**
```bash
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -d '{
    "gitHubUrl": "https://github.com/spring-projects/spring-boot"
  }'
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "gitHubUrl": "https://github.com/spring-projects/spring-boot",
  "repositoryName": "spring-boot",
  "createdAt": "2024-06-05T10:30:00",
  "updatedAt": "2024-06-05T10:30:00",
  "currentStage": "CLONING",
  "statusMessage": "Running Repository Clone",
  "retryCount": 0,
  "vulnerabilities": []
}
```

---

### 2. List All Scans (Paginated)
**Endpoint:** `GET /scans?page=0&size=20`

**Description:** Get paginated list of all scans (most recent first)

**Request:**
```bash
# Get first page (default 20 items)
curl http://localhost:8080/api/scans

# Get specific page
curl "http://localhost:8080/api/scans?page=0&size=10"

# With pagination
curl "http://localhost:8080/api/scans?page=1&size=5"
```

**Response:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "gitHubUrl": "https://github.com/spring-projects/spring-boot",
      "repositoryName": "spring-boot",
      "currentStage": "COMPLETED",
      "vulnerabilities": [
        {
          "id": "uuid-1",
          "severity": "HIGH",
          "type": "Dependency Vulnerability",
          "description": "Log4j RCE Vulnerability"
        }
      ]
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20
}
```

---

### 3. Get Scan Details
**Endpoint:** `GET /scans/{scanId}`

**Description:** Get detailed information about a specific scan including vulnerabilities and recommendations

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

curl http://localhost:8080/api/scans/$SCAN_ID
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "gitHubUrl": "https://github.com/spring-projects/spring-boot",
  "repositoryName": "spring-boot",
  "clonedPath": "/tmp/remedify-scans/550e8400",
  "createdAt": "2024-06-05T10:30:00",
  "updatedAt": "2024-06-05T10:45:30",
  "currentStage": "COMPLETED",
  "statusMessage": "Scan completed successfully",
  "retryCount": 0,
  "vulnerabilities": [
    {
      "id": "vuln-uuid-1",
      "filePath": "pom.xml",
      "severity": "HIGH",
      "type": "Log4j RCE",
      "description": "Apache Log4j2 versions < 2.17.1 are vulnerable to RCE",
      "cveId": "CVE-2021-44228",
      "source": "OWASP",
      "aiRecommendation": {
        "id": "rec-uuid-1",
        "suggestion": "Update Log4j to version 2.17.1 or later",
        "estimatedEffort": "Low",
        "appliedManually": false
      }
    }
  ],
  "testResult": {
    "buildSuccess": true,
    "testsPassed": 245,
    "testsFailed": 0
  }
}
```

---

### 4. Get HTML Report
**Endpoint:** `GET /scans/{scanId}/report` (Accept: text/html)

**Description:** Get the scan report in HTML format

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

curl -H "Accept: text/html" \
  http://localhost:8080/api/scans/$SCAN_ID/report > report.html

# Open in browser
open report.html
```

**Response:**
```html
<!DOCTYPE html>
<html>
  <head>
    <title>Remedify Scan Report - spring-boot</title>
    <!-- Linear Design System styling -->
  </head>
  <body>
    <!-- Full HTML report with vulnerabilities, AI recommendations, test results -->
  </body>
</html>
```

---

### 5. Get JSON Report
**Endpoint:** `GET /scans/{scanId}/report` (Accept: application/json)

**Description:** Get the scan report in JSON format (programmatic access)

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

curl -H "Accept: application/json" \
  http://localhost:8080/api/scans/$SCAN_ID/report | jq .
```

**Response:**
```json
{
  "scanId": "550e8400-e29b-41d4-a716-446655440000",
  "repositoryName": "spring-boot",
  "scanDate": "2024-06-05T10:45:30",
  "summary": {
    "totalVulnerabilities": 5,
    "highSeverity": 2,
    "medium": 2,
    "low": 1,
    "testsPassed": 245,
    "testsFailed": 0,
    "buildSuccess": true
  },
  "vulnerabilities": [
    {
      "severity": "HIGH",
      "type": "Log4j RCE",
      "location": "pom.xml",
      "recommendation": "Update to version 2.17.1 or later"
    }
  ]
}
```

---

### 6. Download Report
**Endpoint:** `GET /scans/{scanId}/report/download?format=html|json`

**Description:** Download report file (returns as attachment)

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

# Download HTML report
curl -O http://localhost:8080/api/scans/$SCAN_ID/report/download?format=html

# Download JSON report
curl -O http://localhost:8080/api/scans/$SCAN_ID/report/download?format=json

# Save with custom filename
curl http://localhost:8080/api/scans/$SCAN_ID/report/download?format=html \
  -o my-scan-report.html
```

---

### 7. Retry Failed Scan
**Endpoint:** `POST /scans/{scanId}/retry`

**Description:** Retry a scan that failed at any stage (max 2 auto-retries)

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

curl -X POST http://localhost:8080/api/scans/$SCAN_ID/retry \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "gitHubUrl": "https://github.com/spring-projects/spring-boot",
  "currentStage": "CLONING",
  "statusMessage": "Retrying: Running Repository Clone",
  "retryCount": 1
}
```

---

### 8. Delete Scan
**Endpoint:** `DELETE /scans/{scanId}`

**Description:** Delete a scan and clean up associated files (repos, temp files)

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

curl -X DELETE http://localhost:8080/api/scans/$SCAN_ID
```

**Response:** `204 No Content` (success)

---

### 9. Subscribe to Real-Time Updates (Server-Sent Events)
**Endpoint:** `GET /scans/{scanId}/sse`

**Description:** Real-time stream of scan progress updates (used by frontend)

**Request:**
```bash
SCAN_ID="550e8400-e29b-41d4-a716-446655440000"

# Using curl (will stream events)
curl http://localhost:8080/api/scans/$SCAN_ID/sse

# Using EventSource in JavaScript (see frontend/src/services/api.ts)
const eventSource = new EventSource(`/api/scans/${scanId}/sse`)
eventSource.onmessage = (event) => {
  const update = JSON.parse(event.data)
  console.log(`Stage: ${update.stage}, Message: ${update.message}`)
}
```

**Event Stream:**
```
event: message
data: {"stage":"CLONING","message":"Cloning repository..."}

event: message
data: {"stage":"SCANNING","message":"Running vulnerability detection..."}

event: message
data: {"stage":"RECOMMENDING","message":"Generating AI recommendations..."}

event: message
data: {"stage":"VALIDATING","message":"Running build and tests..."}

event: message
data: {"stage":"REPORTING","message":"Generating report..."}

event: message
data: {"stage":"COMPLETED","message":"Scan completed successfully"}
```

---

### Quick Reference: Common Workflows

**Create and monitor a scan:**
```bash
# 1. Create scan
RESPONSE=$(curl -s -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -d '{"gitHubUrl":"https://github.com/spring-projects/spring-boot"}')

SCAN_ID=$(echo $RESPONSE | jq -r '.id')
echo "Created scan: $SCAN_ID"

# 2. Check status (poll every 5 seconds)
for i in {1..60}; do
  STATUS=$(curl -s http://localhost:8080/api/scans/$SCAN_ID | jq -r '.currentStage')
  echo "Status: $STATUS"
  [ "$STATUS" = "COMPLETED" ] && break
  sleep 5
done

# 3. Get report
curl -H "Accept: application/json" \
  http://localhost:8080/api/scans/$SCAN_ID/report | jq .

# 4. Download report
curl http://localhost:8080/api/scans/$SCAN_ID/report/download?format=html \
  -o report.html
```

**Batch process multiple repositories:**
```bash
REPOS=(
  "https://github.com/spring-projects/spring-boot"
  "https://github.com/kubernetes/kubernetes"
  "https://github.com/torvalds/linux"
)

for REPO in "${REPOS[@]}"; do
  echo "Scanning: $REPO"
  curl -s -X POST http://localhost:8080/api/scans \
    -H "Content-Type: application/json" \
    -d "{\"gitHubUrl\":\"$REPO\"}" | jq -r '.id'
done
```

**Monitor all scans in progress:**
```bash
watch -n 5 'curl -s http://localhost:8080/api/scans | jq ".content[] | {name: .repositoryName, stage: .currentStage}"'
```

---

## 🎨 Linear Design System

The frontend uses the [Linear Design System](./design.md):
- **Canvas**: `#010102` (dark background)
- **Primary Accent**: `#5e6ad2` (lavender)
- **Surface Levels**: 4-step hierarchy for elevation
- **Typography**: Aggressive negative tracking on display text

All design tokens are configured in `frontend/tailwind.config.js`.

---

## 🔑 Environment Variables

### Required
```bash
ANTHROPIC_API_KEY=sk-ant-...  # Claude API key (required for AI recommendations)
```

### Optional
```bash
SNYK_API_KEY=...              # Snyk vulnerability scanning (if enabled)
```

### Backend Defaults (in application.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:remedifydb  # In-memory H2 database
  jpa:
    hibernate:
      ddl-auto: create-drop      # Auto-create schema
```

---

## 📚 Development Workflow

### 1. Make Changes
- **Backend**: Edit files in `src/main/java/com/remedify/`
- **Frontend**: Edit files in `frontend/src/`

### 2. Test Locally
- Backend auto-reloads on restart: `mvn spring-boot:run`
- Frontend hot-reloads: `npm run dev`

### 3. Verify Both Services
```bash
# Check backend is up
curl http://localhost:8080/api/scans

# Check frontend is up
curl http://localhost:5173
```

### 4. Debug Issues
- **Backend logs**: Terminal running `mvn spring-boot:run`
- **Frontend logs**: Terminal running `npm run dev`
- **Browser console**: Press F12 in browser, check Console tab
- **Backend H2 console**: http://localhost:8080/h2-console (username: `sa`, no password)

---

## 🚨 Troubleshooting

### Port Already in Use
```bash
# Check what's using the port
lsof -i :8080    # Backend
lsof -i :5173    # Frontend

# Kill the process
kill -9 <PID>
```

### CORS Errors
If the frontend can't reach the backend:
1. Verify backend is running on `http://localhost:8080`
2. Check `frontend/vite.config.ts` proxy configuration
3. Check backend CORS settings in `src/main/java/com/remedify/config/CorsConfig.java`

### Claude API Errors
```bash
# Verify API key is set
echo $ANTHROPIC_API_KEY

# Check it's in application.yml
cat src/main/resources/application.yml | grep api-key
```

### Maven Compilation Errors
```bash
# Clean and rebuild
mvn clean compile

# See detailed error messages
mvn clean compile -e
```

### npm Install Issues
```bash
# Clear cache and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install
```

---

## 📖 Further Reading

- **CLAUDE.md** — Architecture and development guidance
- **design.md** — Linear Design System specification
- **Plan File** — `/Users/asutoshpandya/.claude/plans/merry-yawning-clover.md`

---

## 📝 Next Steps

1. ✅ Backend scaffolded (Spring Boot, entities, services, controllers)
2. ✅ Frontend scaffolded (React, Kanban board UI, API client)
3. ⏳ **Implement**:
   - Git clone integration
   - OWASP vulnerability scanning
   - Claude API fix recommendations
   - Maven build/test validation
   - HTML/JSON report generation
   - Real-time SSE updates

---

## 🤝 Contributing

All code follows conventions documented in `CLAUDE.md`:
- Package structure: Spring Boot standard
- Naming: CamelCase for classes, kebab-case for files
- Logging: SLF4J via Spring
- Comments: Minimal; prefer clear naming

---

**Happy Scanning! 🚀**
