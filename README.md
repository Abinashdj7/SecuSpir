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

The backend includes comprehensive unit and integration tests across controllers, services, and security components.

### ✅ Test Summary

* **Total Tests:** 35
* **Failures:** 0
* **Errors:** 0
* **Skipped:** 1
* **Build Status:** ✅ SUCCESS

### 📦 Test Coverage Areas

* **Controller Layer**

  * `TransactionControllerTest`

* **Service Layer**

  * `AccountServiceTest`
  * `AuthServiceTest`
  * `TransactionServiceTest`

* **Security**

  * `JwtUtilTest`

* **Application Context**

  * `SecurebankApplicationTests` (basic context load)

### ▶️ Run Tests

```bash
./mvnw test
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

Triggers when files under `Server/` change.

| Step | Detail |
|------|--------|
| Checkout | `actions/checkout@v4` |
| Java setup | Java 21 (Temurin) with Maven cache |
| Fix permissions | `chmod +x mvnw` |
| Unit tests | `./mvnw test` |
| Build JAR | `./mvnw package -DskipTests` |
| Upload artifact | `securebank-backend` (7-day retention) |

### Frontend CI ([`.github/workflows/frontend.yml`](.github/workflows/frontend.yml))

Triggers when files under `Client/` change.

| Step | Detail |
|------|--------|
| Checkout | `actions/checkout@v4` |
| Node.js setup | Node 20 with npm cache |
| Install deps | `npm ci` |
| Build | `npm run build` |
| Upload artifact | `securebank-frontend` (7-day retention) |

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
