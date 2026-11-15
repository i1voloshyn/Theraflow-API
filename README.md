# TheraFlow API

### A backend for managing a private therapy practice

**TheraFlow** is a REST API written in **Java and Spring Boot**. The goal is to help therapists organize their work and support patients and their guardians.

The application currently supports account registration, authentication, and guardian profile creation. Patient care and appointment features are planned for later.

> **Project status:** Under active development. Available features are listed below.

## Current Features

- Registration for guardian and therapist accounts, with email verification before sign-in.
- Sign-in with JWT access tokens and refresh tokens.
- Refresh token rotation: each refresh returns a new token pair, and the previous refresh token can no longer be used.
- Password reset by email with a link that expires and can be used once. A successful reset also disables existing refresh tokens.
- Creating a guardian profile with a first and last name, linked to the signed-in guardian account.
- Request validation and consistent error responses.
- API documentation through Swagger UI.

Therapist accounts can register and sign in, but therapist profiles are not implemented yet. Guardian profiles currently support creation only.

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Backend | Spring Boot 4.1, Spring Web |
| Authentication | Spring Security, JWT |
| Database access | Spring Data JPA / Hibernate, PostgreSQL |
| Database migrations | Flyway |
| Email | Spring Mail |
| Validation and mapping | Jakarta Bean Validation, MapStruct |
| API documentation | Springdoc OpenAPI, Swagger UI |
| Testing | JUnit, Mockito, AssertJ, Testcontainers, GreenMail |
| Build and local development | Maven, Docker Compose |

## Planned Features

- **Therapist profiles** — create, view, and update therapist details and addresses.
- **Appointment slots and bookings** — manage therapist availability and schedule sessions.
- **Patient profiles** — manage patient information and connect patients with their guardians.
- **Medical records** — keep therapy notes, session history, and progress updates.
- **Guardian profile updates** — view and edit an existing guardian profile.
- **Tasks for patients** — assign exercises and track their completion between sessions.
- **Communication with guardians** — share updates and discuss patient progress.
- **Video lessons** — a possible future addition for remote support.