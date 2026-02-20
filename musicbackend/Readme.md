# Music Backend

This is a Spring Boot application built with Kotlin and Gradle.

## Running the application

You can run the application using the Gradle wrapper included in the project.

```bash
./gradlew bootRun
```

The application will start on port 8002 by default.
## CORS Configuration

When you deploy, you can override this property without changing the code. 
For example, if you deploy your Angular app to https://www.your-music-app.com, 
you would set an environment variable for your Spring Boot container:

`APP_CORS_ALLOWED-ORIGINS=https://www.your-music-app.com`

### Supporting Multiple Origins

Yes, multiple origins can be comma-separated. The way the `CorsConfig.kt` is set up, Spring Boot will automatically handle a comma-separated list for you.
When you define the property in `application.properties` like this: 
`app.cors.allowed-origins=http://localhost:4200,https://your-app.com,https://staging.your-app.com`

The `@Value` annotation injects this into the `allowedOrigins: Array<String>`, and Spring automatically splits the comma-separated string into an array of individual origins. The `allowedOrigins(*allowedOrigins)` call then correctly registers each one.

## Database

By default, the application uses an H2 in-memory database. You can access 
the H2 console at `/h2-console` when running with the `qa` profile.
