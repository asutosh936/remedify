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

## 📡 API Endpoints

### Health & Status
```
GET  /api/scans              # List all scans (paginated)
POST /api/scans              # Create new scan
GET  /api/scans/{scanId}     # Get scan details
```

### Reports
```
GET  /api/scans/{scanId}/report           # Get report (HTML/JSON)
GET  /api/scans/{scanId}/report/download  # Download report
```

### Real-time Updates
```
GET  /api/scans/{scanId}/sse  # Server-Sent Events stream
```

### Examples
```bash
# Create a new scan
curl -X POST http://localhost:8080/api/scans \
  -H "Content-Type: application/json" \
  -d '{"gitHubUrl":"https://github.com/spring-projects/spring-boot"}'

# List scans
curl http://localhost:8080/api/scans

# Get scan details
curl http://localhost:8080/api/scans/{scanId}
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
