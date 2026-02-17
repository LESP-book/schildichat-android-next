导航: `../CLAUDE.md` > `tests/`

# tests/* (QA modules)

## Responsibility

- Repository-level quality and testing helper modules.

## Modules

- `tests/detekt-rules`: custom detekt rules
- `tests/konsist`: architecture/structure checks (Konsist)
- `tests/uitests`: UI/screenshot-related tests
- `tests/testutils`: shared test utilities

## Common Entrypoints

- Aggregated checks task: `./gradlew runQualityChecks` (defined in `build.gradle.kts`)
