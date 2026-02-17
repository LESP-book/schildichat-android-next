导航: `../CLAUDE.md` > `appnav/`

# :appnav (navigation glue)

## Responsibility

- Central navigation and intent/deeplink dispatch.
- Depends on feature *API* modules to create feature nodes.

## Entrypoints

- Root flow node: `appnav/src/main/kotlin/io/element/android/appnav/RootFlowNode.kt`
- Logged-in flow: `appnav/src/main/kotlin/io/element/android/appnav/LoggedInAppScopeFlowNode.kt`

## Dependencies

- Feature entrypoints: `features/*/api` (e.g. `features/login/api/.../LoginEntryPoint.kt`)
- Core infra: `libraries/architecture`, `libraries/matrix/api`, `libraries/oidc/api`, `libraries/deeplink/api`

## DI

- Metro DI is enabled via `setupDependencyInjection()` in `appnav/build.gradle.kts`.
