# 🏦 SecureBank

![Backend CI](https://github.com/Abinashdj7/SecuSpir/actions/workflows/backend.yml/badge.svg)
![Frontend CI](https://github.com/Abinashdj7/SecuSpir/actions/workflows/frontend.yml/badge.svg)

A full-stack digital banking application built with **Angular (frontend)** and **Spring Boot (backend)**. SecureBank provides account management, authentication, and transaction handling in a modern client-server architecture.

---

## 📌 Project Overview

SecureBank allows users to:

* Register and authenticate securely using JWT
* Manage bank accounts (create, view, freeze)
* Perform financial transactions (deposit, withdraw, transfer)
* View transaction history

---

## ✨ Features

### 🔐 Authentication & Security

* JWT-based authentication
* BCrypt password hashing
* Route guards & HTTP interceptors

### 👤 User Management

* User registration & login
* Basic role-based access

### 💳 Account Management

* Create accounts (Checking / Savings)
* View accounts
* Freeze accounts

### 💸 Transactions

* Deposit / Withdraw / Transfer
* Transaction history

### 📊 Dashboard

* Balance & activity overview

---

## 🧰 Tech Stack

**Frontend**

* Angular, TypeScript, Tailwind CSS, RxJS

**Backend**

* Spring Boot, Spring Security, JWT, JPA, MySQL

**DevOps**

* Docker, Docker Compose, Nginx

---

## 🏗️ Architecture

```
[ Angular Frontend ]
        |
        | HTTP (JWT)
        v
[ Spring Boot Backend ]
        |
        v
     [ MySQL DB ]
```

---

## 🚀 Installation

### Prerequisites

* Node.js (v18+)
* Java 17+
* Maven
* Docker

### Run with Docker

```bash
docker-compose up --build
```

* Frontend → http://localhost:4200
* Backend → http://localhost:8080
* MySQL → localhost:3307

### Manual Setup

**Backend**

```bash
cd server
./mvnw spring-boot:run
```

**Frontend**

```bash
cd client/securebank-frontend
npm install
npm start
```

---

## 🔑 Environment Variables

| Variable                   | Description      |
| -------------------------- | ---------------- |
| SPRING_DATASOURCE_URL      | DB URL           |
| SPRING_DATASOURCE_USERNAME | DB user          |
| SPRING_DATASOURCE_PASSWORD | DB password      |
| JWT_SECRET                 | JWT secret       |
| JWT_EXPIRATION             | Token expiration |

---

## 📜 Scripts

**Frontend**

```bash
npm start
npm run build
npm test
```

**Backend**

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

---

## 🧪 Testing

The project has five test layers: backend unit, backend integration, replication/failover (Spring), frontend unit, and end-to-end — 122 tests in total.

| Suite | Tool | Count | Command |
|---|---|---|---|
| Backend unit | JUnit 5 + Mockito | 34 | `./mvnw test -Dtest="!*IntegrationTest,!SecurebankApplicationTests,!DatabaseReplicationTest"` |
| Backend integration | `@SpringBootTest` + H2 | 33 | `./mvnw test -Dtest="*IntegrationTest,SecurebankApplicationTests"` |
| Replication routing & failover | JUnit 5 + H2 | 7 | `./mvnw test -Dtest="DatabaseReplicationTest"` |
| Frontend unit | Vitest | 10 | `npm test` (in `Client/securebank-frontend`) |
| E2E | Cypress | 38 | `npm run e2e` (in `Client/securebank-frontend`) |

### Backend (JUnit 5 + Mockito + H2)

* **Unit tests** — `AccountControllerTest`, `AuthControllerTest`, `TransactionControllerTest`, `AccountServiceTest`, `AuthServiceTest`, `TransactionServiceTest`, `JwtUtilTest`
* **Integration tests** (`@SpringBootTest` + H2 in-memory DB) — `AuthIntegrationTest`, `AccountIntegrationTest`, `TransactionIntegrationTest`, `SecurebankApplicationTests`
* **Replication tests** (`DatabaseReplicationTest`) — verifies read/write routing to primary/replica and failover behaviour when the primary datasource is brought down

```bash
cd Server

# Unit tests only
./mvnw test -Dtest="!*IntegrationTest,!SecurebankApplicationTests,!DatabaseReplicationTest" \
  -Dsurefire.failIfNoSpecifiedTests=false

# Integration tests only
./mvnw test -Dtest="*IntegrationTest,SecurebankApplicationTests" \
  -Dsurefire.failIfNoSpecifiedTests=false

# Replication routing & failover tests only
./mvnw test -Dtest="DatabaseReplicationTest" -Dsurefire.failIfNoSpecifiedTests=false

# Everything
./mvnw test
```

### Frontend (Vitest + Cypress)

* **Unit tests** — services, guards, interceptors, and components (`auth`, `account`, `transaction`, `transfer`, `transaction-history`)
* **E2E tests** (`cypress/e2e/`) — `auth.cy.ts`, `dashboard.cy.ts`, `transfer.cy.ts`, `transactions.cy.ts`

```bash
cd Client/securebank-frontend

# Unit tests
npm test

# E2E tests (starts the dev server automatically)
# Note: unset ELECTRON_RUN_AS_NODE first if running inside VS Code's integrated terminal
unset ELECTRON_RUN_AS_NODE && npm run e2e
```

### Live MySQL Replication Failover Test

A standalone script that exercises the real Docker containers end-to-end:

1. Confirms replication is active (IO + SQL threads running, lag = 0 s)
2. Writes 100 rows to the primary
3. Verifies all 100 rows appear on the replica
4. Stops the primary container (`docker stop`)
5. Confirms the replica still serves all rows with zero data loss
6. Restarts the primary so it rejoins as replication source

```bash
# Start the containers first (if not already running)
docker compose up -d mysql-primary mysql-replica mysql-replication-setup

# Run the failover test
bash scripts/replication-failover-test.sh
```

Example output:

```
========================================================
  MySQL Replication Failover Test
========================================================

[1/5] Replication status
  PRIMARY   securebank-mysql-primary  (localhost:3310)
  REPLICA   securebank-mysql-replica  (localhost:3311)
  Replica lag: 0s

[2/5] Writing 100 rows to primary
  Inserted  : 100 rows
  Write time: 425 ms

[3/5] Pre-failover read check (replica)
  Rows readable on replica before failover: 100

[4/5] Stopping primary: securebank-mysql-primary
  securebank-mysql-primary stopped  (10216 ms)

[5/5] Post-failover read check (replica)
  Rows readable on replica after failover : 100
  Read time after failover                : 662 ms

  Restarting securebank-mysql-primary (rejoins as replication primary)...
  securebank-mysql-primary restarted

========================================================
  RESULTS
========================================================
  Primary container           : securebank-mysql-primary (localhost:3310)
  Replica container           : securebank-mysql-replica (localhost:3311)
  Rows written                : 100
  Write time                  : 425 ms
  Rows before failover        : 100
  Rows after failover         : 100
  Data loss                   : 0 rows
  Total test duration         : 23857 ms

  RESULT: PASS — zero data loss, replica survived primary failure

========================================================
```

---

## 🔌 API Overview

### Auth

* POST `/api/auth/register`
* POST `/api/auth/login`

### Accounts

* GET `/api/accounts`
* GET `/api/accounts/{id}`
* POST `/api/accounts`
* PATCH `/api/accounts/{id}/freeze`

### Transactions

* GET `/api/accounts/{id}/transactions`
* POST `/deposit`
* POST `/withdraw`
* POST `/transfer`

---

## 📁 Structure

**Frontend**

```
src/app/
├── components/
├── services/
├── guards/
└── interceptors/
```

**Backend**

```
com/securebank/
├── controller/
├── service/
├── repo/
├── security/
└── model/
```

---

## 🚢 Deployment

* Nginx serves frontend
* Spring Boot runs backend
* MySQL uses Docker volume

---

## ⚙️ GitHub Actions (CI)

Two path-filtered workflows run on every push or pull request to `main`.

### Backend CI ([`.github/workflows/backend.yml`](.github/workflows/backend.yml))

Triggers when files under `Server/` change. Three jobs run in sequence:

| Job | Detail |
|-----|--------|
| Unit Tests | Java 21 (Temurin) + Maven cache → `./mvnw test` excluding `*IntegrationTest`/`SecurebankApplicationTests`; uploads `unit-test-report` |
| Integration Tests | `./mvnw test` for `*IntegrationTest` + `SecurebankApplicationTests` (H2 in-memory DB); uploads `integration-test-report` |
| Build JAR | Runs after both test jobs pass → `./mvnw package -DskipTests`; uploads `securebank-backend` (7-day retention) |

### Frontend CI ([`.github/workflows/frontend.yml`](.github/workflows/frontend.yml))

Triggers when files under `Client/` change. Three jobs run in sequence:

| Job | Detail |
|-----|--------|
| Unit Tests | Node 20 + npm cache → `npm test` (Vitest) |
| Build | `npm run build` (Angular); uploads `securebank-frontend` dist (7-day retention) |
| E2E Tests | Runs after Unit Tests + Build pass → Cypress against `npm start`; uploads screenshots on failure |

> Neither workflow includes deployment steps — they stop at build/test.

---

## 🔮 Future Improvements

* Full RBAC (Admin/User)
* Refresh tokens
* Email notifications
* Better test coverage
* Analytics dashboard

---

## ⚠️ Disclaimer

This is a **demo/learning project** and not production-ready without further security hardening.

---

## 👨‍💻 Author

Abinash
