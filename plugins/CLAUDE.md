导航: `../CLAUDE.md` > `plugins/`

# plugins/ (composite build for Gradle build logic)

## Responsibility

- Houses precompiled Gradle script plugins and build logic.
- Included via `includeBuild("plugins")` in `settings.gradle.kts`.

## Key Files

- Settings: `plugins/settings.gradle.kts`
- Build: `plugins/build.gradle.kts`
- Common build extensions live under `plugins/src/main/kotlin/`.

## Notable Build Extensions

- Metro DI wiring: `plugins/src/main/kotlin/extension/DependencyInjectionExtensions.kt`
