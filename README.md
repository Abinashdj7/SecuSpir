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

The project has unit, integration, and end-to-end test coverage on both backend and frontend, all wired into CI.

### Backend (JUnit 5 + Mockito + H2)

* **Unit / slice tests** — `AccountControllerTest`, `AuthControllerTest`, `TransactionControllerTest`, `AccountServiceTest`, `AuthServiceTest`, `TransactionServiceTest`, `JwtUtilTest`
* **Integration tests** (`@SpringBootTest` + H2 in-memory DB) — `AuthIntegrationTest`, `AccountIntegrationTest`, `TransactionIntegrationTest`, `SecurebankApplicationTests`

```bash
# Unit tests only
./mvnw test -Dtest="!*IntegrationTest,!SecurebankApplicationTests"

# Integration tests only
./mvnw test -Dtest="*IntegrationTest,SecurebankApplicationTests"

# Everything
./mvnw test
```

### Frontend (Vitest + Cypress)

* **Unit tests** — services, guards, interceptors, and components (`auth`, `account`, `transaction`, `transfer`, `transaction-history`)
* **E2E tests** (`cypress/e2e/`) — `auth.cy.ts`, `dashboard.cy.ts`, `transfer.cy.ts`, `transactions.cy.ts`

```bash
# Unit tests
npm test

# E2E tests (starts the dev server automatically)
npm run e2e
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
