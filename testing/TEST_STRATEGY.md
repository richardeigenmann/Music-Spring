# Music-Spring Test Strategy

## 1. Overview
The testing strategy for the Music-Spring application prioritizes functional correctness, data integrity, and regression prevention. Given the target environment (single-user, <10k tracks), focus is shifted towards ensuring that core data operations—importing, classifying, and playback—remain reliable as the application evolves.

## 2. Testing Levels & Tools

| Level | Focus | Tools |
| :--- | :--- | :--- |
| **Unit** | Core logic (import algorithms, data mapping, service methods). | Kotlin: `JUnit 5`, `MockK`; TS: `Jasmine` |
| **Integration** | Backend-DB interaction, API contract validation. | `Spring Boot Test`, `Testcontainers` (PostgreSQL) |
| **Component** | Frontend UI state, component interaction. | `Karma`/`Jasmine` (Angular) |
| **E2E** | Full user flows (Import -> Search -> Play). | `Playwright` |

## 3. Execution Strategy

### Phase 1: Foundations & Regressions
- **CI/CD:** Automate `gradle test` and `ng test` on every push.
- **Integration Testing:** Prioritize `MusicImportService` and `TrackDataService` to protect data import integrity.

### Phase 2: Functional E2E Testing
- Focus on "Happy Path" scenarios using `Cypress` to ensure cross-layer functionality (e.g., successful import leading to UI updates).

### Phase 3: Exploratory & Usability
- Manual verification of edge cases in metadata/ID3 parsing and UI responsiveness.

## 4. Specialized Considerations
- **Load/Volume:** No formal load testing; limited to a volume "smoke test" at 10k tracks to ensure sub-200ms query performance.
- **Data Integrity:** Run validation scripts post-import to ensure no orphaned `TrackFile` records and consistent `TrackGroup` relationships.
