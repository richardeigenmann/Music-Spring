# Music-Spring Application

This project is a full-stack music management application with a Spring Boot Kotlin backend and an Angular frontend.

## Quick Start with Docker Compose

To get the application up and running quickly using Docker Compose, follow these steps:

### 1. Prerequisites
- Docker and Docker Compose installed on your host machine.
- Your host machine should be accessible via its hostname (e.g., `octan`).

### 2. Customize `docker-compose.yaml`
Before running the application, you **must** update the `docker-compose.yaml` file with your specific configuration:

- **Image Names:** Update `image: richardeigenmann/musicbackend:latest` and `image: richardeigenmann/musicfrontend:latest` to point to your actual Docker Hub repository and image tags.
- **Hostname:** If your host's name is not `octan`, search and replace `octan` with your actual hostname in the environment variables:
  - `APP_CORS_ALLOWED_ORIGINS` for the `backend` service.
  - `BACKEND_URL` for the `frontend` service.
- **Paths:** Ensure the host paths `/richi/mp3` and `/richi/ToDo` exist on your machine and contain your music files. If they are in a different location, update the `volumes` section for the `backend` service.
- **Database Credentials:** You can change the `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_DB` values. Just make sure the corresponding `SPRING_DATASOURCE_*` variables in the `backend` service match.

### 3. Run the application
In the root directory of the project, run:
```bash
docker-compose up -d
```

### 4. Access the Application
- **Frontend:** [http://octan:8010](http://octan:8010)
- **Backend API:** [http://octan:8011/api](http://octan:8011/api) (with Swagger at `/swagger-ui.html`)

## Architecture Notes
- The **PostgreSQL** database stores track metadata and playlist information.
- The **Backend** (port 8011) handles music file scanning, metadata extraction, and provides the REST API.
- The **Frontend** (port 8010) provides the user interface to browse, search, and manage playlists.

## Backend Technical Requirement
Ensure your backend image includes the PostgreSQL JDBC driver. If you're building it from the provided source, add the following dependency to `musicbackend/build.gradle`:
```gradle
runtimeOnly 'org.postgresql:postgresql'

docker compose up -d
```
