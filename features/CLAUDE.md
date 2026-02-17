导航: `../CLAUDE.md` > `features/`

# features/* (UI features)

## Responsibility

- Each directory under `features/<name>/` represents a user-facing feature (screen or flow).
- Typical module split:
  - `features/<name>/api`: public entrypoint interfaces (e.g. `*EntryPoint`)
  - `features/<name>/impl`: implementation, UI, DI bindings
  - `features/<name>/test`: unit/screenshot helpers
  - `features/<name>/shared`: shared types (when present)

## Public Surface Pattern

- Look for `FeatureEntryPoint` / `*EntryPoint` in `features/<name>/api/src/main/kotlin/**`.
- These entrypoints usually expose `createNode(...)` for Appyx navigation.

## How to Navigate into a Feature

1) Start at `appnav/src/main/kotlin/io/element/android/appnav/RootFlowNode.kt`.
2) Find the navigation target/intent mapping.
3) Follow the referenced `features/<name>/api` entrypoint.
4) Implementation is in `features/<name>/impl`.

## Current Feature Directories (first-level)

analytics, announcement, cachecleaner, call, createroom, deactivation, enterprise, forward, ftue, home, invite, invitepeople, joinroom, knockrequests, leaveroom, licenses, location, lockscreen, login, logout, messages, migration, networkmonitor, poll, preferences, rageshake, reportroom, rolesandpermissions, roomaliasresolver, roomcall, roomdetails, roomdirectory, roommembermoderation, securebackup, securityandprivacy, share, signedout, space, startchat, userprofile, verifysession, viewfolder.
