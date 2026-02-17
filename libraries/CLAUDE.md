导航: `../CLAUDE.md` > `libraries/`

# libraries/* (shared libraries)

## Responsibility

- Shared infrastructure and reusable components.
- Common split:
  - `libraries/<name>/api`: public surface
  - `libraries/<name>/impl`: implementation and DI
  - `libraries/<name>/test`: test helpers
- Some libraries are single-module: `libraries/<name>/build.gradle.kts`.

## Important Libraries

- DI helpers: `libraries/di`, `libraries/architecture`
- Design system / UI: `libraries/designsystem`, `libraries/ui-common`, `libraries/ui-strings`, `libraries/matrixui`
- Matrix integration:
  - `libraries/matrix/api` (public Matrix client surface)
  - `libraries/matrix/impl` (implementation)
  - `libraries/rustsdk` (local AAR override support)

## DI Pattern

- Look for `@ContributesTo(AppScope::class)` / `@ContributesTo(RoomScope::class)` in `impl` modules.
