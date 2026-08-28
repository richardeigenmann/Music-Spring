# Music-Spring Project Context

## Project Overview
Music-Spring is a full-stack music management application allowing users to host, organize, and play their MP3 collections. It includes a web frontend, an Android client, and a robust backend.

## Tech Stack
- **Backend**: Kotlin, Spring Boot, Spring Data JPA
- **Database**: PostgreSQL (main), H2 (in-memory/dev)
- **Web Frontend**: Angular (v21+), TypeScript
- **Android Client**: Native Android (Kotlin)
- **Build System**: Gradle (Multi-project)
- **Infrastructure**: Docker Compose, Helm, Kubernetes (OpenShift/CRC)
- **Testing**: Cypress (E2E), JUnit (Backend), Angular Test Runner

## Directory Structure
- `musicbackend/`: Spring Boot Kotlin backend
- `musicfrontend/`: Angular web application
- `musicandroid/`: Android application
- `helm/`: Kubernetes deployment charts
- `docker-compose.yaml`: Local development environment setup

## Conventions
- **API**: RESTful APIs provided by the backend.
- **Specifications**: Follow OpenSpec conventions in the `openspec/` directory.
- **Development**: Use `docker-compose` for local testing with Postgres.
