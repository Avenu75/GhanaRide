# GhanaRide

GhanaRide is a Ghana-based campus and intercity transport booking platform built with Spring Boot, Thymeleaf, MySQL, and Maven.

This rebuild includes:
- full compile-error cleanup
- repository/package cleanup
- Flyway database migrations
- a seeded local admin account
- local MySQL development configuration
- dependency/security refresh for Java 21

## Tech Stack

- Java 21
- Spring Boot 3.5.16
- Spring MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- MySQL 8+
- Flyway
- Maven
- Lombok

## Why Spring Boot 3.5.16?

The original project used Spring Boot 3.2.5. This rebuild upgrades to **Spring Boot 3.5.16** to pick up newer patched Spring, Tomcat, Jackson, Logback, Thymeleaf, and Spring Security dependencies while keeping the codebase on the Spring Boot 3.x line.

I intentionally did **not** jump to Spring Boot 4.x because that is a bigger framework upgrade and would increase the risk of behavior changes in security, third-party compatibility, and runtime wiring. Staying on the latest 3.x patch line keeps the application functionally close to the original while still significantly improving dependency hygiene.

## Key Features

- user registration and login
- Google OAuth login support
- role-based access for passenger, driver, company, and admin
- trip creation and approval workflow
- trip booking with seat-map support
- payment flow scaffolding for wallet, cash, and Paystack
- wallet and wallet transaction history
- notifications
- profile management
- password reset flow
- admin dashboards for trips, users, and bookings
- company and driver portals
- live route / tracking scaffolding

## Project Structure

```text
src/main/java/com/ghanaride
├── config
├── controller
├── dto
├── entity
├── exception
├── init
├── model
├── repository
├── security
└── service

src/main/resources
├── application.properties
├── application-dev.properties
├── application-prod.properties
├── db/migration
├── static
└── templates
```

## Prerequisites

Install the following:
- JDK 21
- MySQL 8+
- Maven 3.9+
- IntelliJ IDEA (recommended)

## Local Development Setup

### 1. Clone the project

```bash
git clone <your-repo-url>
cd GhanaRide
```

### 2. Configure environment variables

Copy `.env.example` values into your OS environment or IntelliJ Run Configuration.

Minimum local values:

```env
DB_URL=jdbc:mysql://localhost:3306/ghanaride?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=root
APP_BASE_URL=http://localhost:8088
ADMIN_PASSWORD=Admin@12345
```

Notes:
- `DB_USERNAME` and `DB_PASSWORD` are the primary local credentials expected by the dev profile.
- If you prefer, you can also use `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.
- `createDatabaseIfNotExist=true` is enabled for convenience in local dev.

### 3. Start MySQL

Create the database manually if you prefer:

```sql
CREATE DATABASE ghanaride;
```

If your MySQL user can create databases, the default connection string can also create it automatically.

### 4. Run the application

```bash
mvn spring-boot:run
```

or from IntelliJ:
- open the `GhanaRide` folder as a project
- let Maven import dependencies
- run `GhanaRideApplication`

### 5. Flyway migrations

Flyway runs automatically at startup.

The database schema is created from:
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__seed_admin.sql`

## Default Local Admin Login

After first startup, you can log in with:

- **Username:** `admin`
- **Email:** `admin@ghanaride.local`
- **Password:** `Admin@12345`

You can override the password with:

```env
ADMIN_PASSWORD=YourOwnStrongPassword
```

## Important Configuration Notes

### Database
- `spring.jpa.hibernate.ddl-auto=validate`
- Flyway owns schema creation
- the app no longer depends on `ddl-auto=update` for initial setup

### Mail
Local defaults point to MailHog-style SMTP:
- host: `localhost`
- port: `1025`

If you are not running a local mail server, the app can still start, but email sending will fail until mail settings are configured.

### OAuth
Google OAuth values default to placeholders for local development. Replace them with real credentials only if you want to test Google login locally.

## Major Rebuild Fixes Included

### Compile and codebase fixes
- removed duplicate DTO/package conflict for `ContactFormDTO`
- removed duplicate password encoder bean definition
- repaired broken imports and missing symbols
- corrected repository/service/controller mismatches
- fixed multiple incomplete service methods that returned `null`
- repaired notification service and wallet notification usage
- corrected booking/trip lookup methods that needed fetch-detail variants
- normalized company entity/repository mapping
- repaired wallet transaction model/repository mismatch (`providerRef`)
- fixed review entity/controller relationship handling
- added missing templates used by routes/email rendering

### Database and schema fixes
- added first-class Flyway schema creation
- added admin seed data migration
- aligned entity usage with database migration structure

### Security and dependency cleanup
- upgraded Spring Boot from `3.2.5` to `3.5.16`
- upgraded `springdoc-openapi-starter-webmvc-ui` to `2.8.17`
- retained Java 21 compatibility
- avoided Spring Boot 4.x because it is a higher-risk breaking upgrade for this codebase

## Recommended Next Steps After Pulling

1. Open the project in IntelliJ.
2. Confirm JDK 21 is selected.
3. Set your database environment variables.
4. Run the application.
5. Log in with the seeded admin account.
6. Verify key flows:
   - register passenger
   - register driver/company
   - create trip
   - approve trip as admin
   - book trip
   - view wallet/notifications

## Known Practical Notes

- Paystack integration is still scaffold-style and depends on real keys for live verification.
- WebSocket/tracking is present but still basic.
- Email templates are intentionally lightweight placeholders to keep the rebuild stable.

## License

This project remains subject to the repository owner's chosen license and terms.
