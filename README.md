# Filmer

## Project Overview
Filmer is a full-stack web application that allows users to browse, search, and rent movies from the IMDb dataset. This project is developed as part of the **SWE 481 (Advanced Web Applications Engineering)** course at King Saud University, College of Computer and Information Sciences.

The project is inspired by the University of California, Irvine course CS122B and follows modern full-stack development practices, emphasizing security, scalability, and software engineering best practices.

### Project Objectives
- Design and implement a fully functional, secure, and scalable web application
- Apply RESTful API principles and modern web development patterns
- Implement database integration with proper schema design
- Develop authentication and authorization mechanisms
- Write and maintain automated tests
- Address performance, security, and scalability concerns
- Follow collaborative development workflows using Git and GitHub

### Key Features
- Movie browsing and search functionality
- Movie rental system
- User authentication and access control
- Responsive user interface
- RESTful API backend
- Secure data handling and HTTPS communication
- Performance-optimized database queries

## Team Members
| Name | Student ID |
|------|------------|
| **Battal Algethanin** | **444100185** |
| **Bader Aldakhil** | **444102492** |
| **Faisal Alangari** | **444101279** |
| **Ibrahim Alathel** | **444105821** |

## Technology Stack
* **Frontend:** Angular (v16+)
* **Backend:** Spring Boot with Java 17
* **Database:** PostgreSQL 15
* **Development Tools:** Docker, Maven, Git, pgAdmin
* **Testing:** JUnit, Jasmine/Karma
* **CI/CD:** GitHub Actions

## Repository Structure
```
Filmer/
├── backend/          # Spring Boot server application
│   ├── src/          # Java source code
│   ├── lib/          # External dependencies
│   ├── data/         # IMDb dataset files
│   └── pom.xml       # Maven configuration
├── frontend/         # Angular client application
├── external/         # External tools (JDK)
├── .github/          # CI/CD workflows and GitHub Actions
├── docker-compose.yaml
└── README.md
```

## Setup & Installation

### One-Command Local Start/Stop (Windows PowerShell)

From the repository root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

This command will:
- Start Docker Desktop (if installed)
- Start PostgreSQL and backend using Docker Compose
- Start Angular frontend in a new PowerShell window
- Open http://localhost:4200/

To stop services:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

Note: close the Angular terminal window manually if it is still running.

### Prerequisites
* **Docker Desktop** - [Download here](https://www.docker.com/products/docker-desktop/)
  - Includes Docker and Docker Compose
  - Works on Windows, Mac, and Linux
* **Java JDK 17+** - [Download here](https://www.oracle.com/java/technologies/downloads/)
* **Node.js & npm** (v18+) - [Download here](https://nodejs.org/)
* **Maven** (v3.8+) - Usually bundled with IDE or [download here](https://maven.apache.org/download.cgi)
* **Git** - [Download here](https://git-scm.com/downloads)

---

### Quick Start with Docker

#### 1. Clone the Repository
```bash
git clone https://github.com/battal-algethanin/Filmer.git
cd Filmer
```

#### 2. Start Services
```bash
docker-compose up -d
```

This will start PostgreSQL and pgAdmin. The database schema will be automatically initialized.

#### 3. Access pgAdmin
- Open browser: **http://localhost:5050**
- Login credentials are configured in `docker-compose.yml`
- Add server connection using details from `docker-compose.yml`

#### 4. Configure Application
Copy the example environment file and configure your local settings:
```bash
cd backend
cp .env.example .env
```

Edit `.env` file with your local database configuration.

#### 5. Run Backend
```bash
cd backend
mvn spring-boot:run
```

#### 6. Run Frontend
```bash
cd frontend
npm install
npm start
```

---

## 🔗 End-to-End Connection Test

This section describes how to verify that your full stack is properly connected: **Angular Frontend → Spring Boot Backend → PostgreSQL Database**.

### Prerequisites for Connection Test
Before running the connection test, ensure the following services are running:

1. **PostgreSQL Database** (via Docker):
   ```bash
   docker-compose up -d postgres
   ```
   - Default hostname: `localhost`
   - Default port: `5432`
   - Database name: `filmer`
   - Username: `postgres`
   - Password: `filmer_dev_password` (as configured in `docker-compose.yaml`)

2. **Spring Boot Backend**:
   ```bash
   cd backend
   export DB_URL="jdbc:postgresql://localhost:5432/filmer"
   export DB_USER="postgres"
   export DB_PASSWORD="filmer_dev_password"
   mvn spring-boot:run
   ```
   - Backend runs on: `http://localhost:8080`
   - Health API endpoint: `http://localhost:8080/api/v1/health`
   - Database connectivity test endpoint: `http://localhost:8080/api/v1/health/db`

3. **Angular Frontend**:
   ```bash
   cd frontend
   npm install
   npm start
   ```
   - Frontend runs on: `http://localhost:4200`
   - Angular dev server opens automatically (or navigate manually)

### Running the Connection Test

Once all three services are running:

1. Open your browser and navigate to: **http://localhost:4200**
2. Click on the **"Connection Test"** link in the navigation menu
3. Click the **"Test Connection"** button

### Expected Results

**Success Scenario:**
- Frontend displays: ✅ **Connection Successful!**
- You'll see a JSON response from the backend containing:
  - `success`: `true`
  - `database_status`: `"UP"`
  - `result`: `1` (from the SELECT 1 query)
  - `message`: `"Database connection successful"`

**Failure Scenarios:**

| Issue | Symptom | Solution |
|-------|---------|----------|
| PostgreSQL not running | Error: "Cannot connect to backend" or "Database connection failed (503)" | Run `docker-compose up -d postgres` |
| Backend not running | Error: "Cannot connect to backend server. Is it running on http://localhost:8080?" | Run `mvn spring-boot:run` in backend folder |
| CORS configuration error | Error: "Access to XMLHttpRequest blocked by CORS policy" | CORS is already configured in backend (src/main/java/com/filmer/config/CorsConfig.java) |
| Environment variables not set | Backend runs but cannot connect to database | Set environment variables: DB_URL, DB_USER, DB_PASSWORD |

### Test Endpoint Details

**Endpoint:** `GET /api/v1/health/db`

**Purpose:** Tests database connectivity by executing a simple `SELECT 1` query

**Request:**
```http
GET http://localhost:8080/api/v1/health/db
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "result": 1,
    "message": "Database connection successful",
    "database_status": "UP"
  }
}
```

**Failure Response (503 Service Unavailable):**
```json
{
  "success": false,
  "error": {
    "code": "DB_CONNECTION_ERROR",
    "message": "Database connection failed"
  }
}
```

### Troubleshooting Connection Issues

1. **Check Docker Services:**
   ```bash
   docker-compose ps
   # Should show postgres and pgadmin as "Up"
   ```

2. **Verify Database is Accessible:**
   ```bash
   docker exec -it filmer-postgres psql -U postgres -d filmer -c "SELECT 1"
   # Should return: 1
   ```

3. **Check Backend is Running:**
   ```bash
   curl http://localhost:8080/api/v1/health
   # Should return: {"success":true,"data":{"status":"UP","database":"UP",...}}
   ```

4. **Check Frontend Network Requests:**
   - Open browser DevTools (F12)
   - Navigate to the Network tab
   - Click "Test Connection" button
   - Look for the request to `http://localhost:8080/api/v1/health/db`
   - Check the response tab for details

5. **View Backend Logs:**
   ```bash
   # Just look at the terminal where you ran: mvn spring-boot:run
   # You should see SQL queries being executed and connection events
   ```

### Next Steps

Once the connection test is successful:
- ✅ Your development environment is properly configured
- ✅ Frontend ↔ Backend communication is working
- ✅ Backend ↔ Database communication is working
- You can now proceed to implement and test application features

---

### 🛠️ Docker Commands Reference

```bash
# Start all services
docker-compose up -d

# Stop all services (keeps data)
docker-compose down

# Stop and remove all data (fresh start)
docker-compose down -v

# View logs
docker-compose logs -f

# View PostgreSQL logs only
docker-compose logs -f postgres

# Restart services
docker-compose restart

# Check service status
docker-compose ps

# Access PostgreSQL CLI directly
docker exec -it filmer-postgres psql -U postgres -d filmer
```

---

### 🔍 Troubleshooting

**Issue:** `Port 5432 is already in use`  
**Solution:** You have PostgreSQL running locally. Either:
- Stop local PostgreSQL: `net stop postgresql-x64-15` (Windows)
- Change Docker port in `docker-compose.yml`: `"5433:5432"`

**Issue:** `Port 5050 is already in use`  
**Solution:** Change pgAdmin port in `docker-compose.yml`: `"5051:80"`

**Issue:** Docker containers won't start  
**Solution:** 
```bash
docker-compose down -v
docker-compose up -d
```

**Issue:** Can't connect to database from Spring Boot  
**Solution:** Verify environment variables are set correctly and match docker-compose.yml

**Issue:** Schema tables not created  
**Solution:** 
```bash
# Recreate database with schema
docker-compose down -v
docker-compose up -d
```

---

### 📋 Development Checklist

Before starting development, ensure:
- [x] Docker Desktop is installed and running
- [x] `docker-compose up -d` executed successfully
- [x] pgAdmin accessible at http://localhost:5050
- [x] Database `filmer` visible in pgAdmin with 9 tables
- [x] Environment variables configured (`.env` file or IDE)
## Useful Commands

### Docker
```bash
docker-compose up -d          # Start services
docker-compose down           # Stop services
docker-compose down -v        # Stop and remove data
docker-compose logs -f        # View logs
docker-compose ps             # Check status
```

### Development
```bash
# Backend
cd backend
mvn clean install
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm start
```

## Troubleshooting

**Port conflicts**: If ports 5432 or 5050 are in use, modify port mappings in `docker-compose.yml`

**Database connection issues**: Verify environment variables match `docker-compose.yml` configuration

**Schema not loading**: Run `docker-compose down -v && docker-compose up -d` to reset database

---

## Environment Configuration

**Important:** This project uses environment variables for sensitive configuration. Never commit credentials, API keys, or secrets to GitHub.

Create a `.env` file in the backend directory with your local database configuration:
```
DB_URL=jdbc:postgresql://localhost:5432/filmer
DB_USER=your_username
DB_PASSWORD=your_password
```

The `.env` file is included in `.gitignore` and will not be committed to the repository.

## Development Workflow

### Branching Strategy
- Each team member works on their own development branch
- All changes must be submitted via Pull Requests (PRs)
- PRs require at least two reviewer approvals before merging
- Direct commits to main branch are blocked

### Code Quality
- All code must pass automated tests before merging
- GitHub Actions run linting, formatting, and testing checks
- Follow clean code principles and proper documentation
- All API endpoints must be documented with request/response formats

## Testing

This project includes comprehensive test coverage:
- **Unit Tests**: Backend services and frontend components
- **Integration Tests**: API endpoints and database operations
- **End-to-End Tests**: Complete user workflows
- **Performance Tests**: Response time and load testing

Run tests before submitting any PR:
```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test
```

### Frontend Unit + E2E (Phase 3)

Run all frontend tests locally from the `frontend/` directory:

```bash
npm ci
```

```bash
# Headless Angular unit tests (non-watch)
npm test
```

```bash
# Install Playwright browser once
npm run e2e:install

# Run E2E suite
npm run e2e
```

Notes:
- `npm test` runs Karma/Jasmine in headless mode for CI-friendly execution.
- `npm run e2e` runs Playwright smoke/flow/negative scenarios.
- E2E assumes backend API is available at `http://localhost:8080` (or mock/intercepted by tests where specified).
- Keep all sensitive config in environment variables; never commit secrets or keys.

### Backend Unit Tests (Phase 3)

Backend unit tests verify service and controller behavior using JUnit5 + Mockito. Tests are isolated from the real database using mocks.

**Test Coverage:**
- **Service Tests**: `GenreServiceTest`, `MovieServiceTest` - test business logic with mocked repositories
- **Controller Tests**: `HealthControllerTest`, `GenreControllerTest` - test HTTP layer with mocked services

**Test Categories:**
- ✅ Success cases (valid inputs, expected outputs)
- ✅ Not found / Invalid input cases (404, 400 responses)
- ✅ Simulated DB exceptions (verify error handling)

**Run Backend Tests:**

```bash
cd backend

# Run all tests
mvn test

# Or using Maven wrapper (if available)
./mvnw test

# Run specific test classes
mvn test -Dtest="GenreServiceTest,MovieServiceTest,HealthControllerTest,GenreControllerTest"
```

**Test Results:**
- Tests run: 36
- Service tests: 23 (GenreService: 10, MovieService: 13)
- Controller tests: 13 (HealthController: 5, GenreController: 8)

**Notes:**
- Tests are designed based on the Phase 2 API specification (see `docs/api-spec.md`)
- Tests may fail meaningfully until Phase 4 implementation is complete
- Tests use `@WebMvcTest` for controller slices and `@ExtendWith(MockitoExtension.class)` for services
- No real database connection is required to run unit tests

## Academic Integrity

This project is developed for educational purposes as part of the SWE 481 course at King Saud University, College of Computer and Information Sciences. All work must follow academic integrity policies.

**Course Instructor:** Dr. Mohammad Abdullah - Alwadud

---

*This project follows the specifications and requirements outlined in the SWE 481 course curriculum and is inspired by UC Irvine's CS122B course.*