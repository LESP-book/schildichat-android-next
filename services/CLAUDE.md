导航: `../CLAUDE.md` > `services/`

# services/* (cross-cutting services)

## Responsibility

- Cross-cutting services consumed by many modules (analytics, error handling, navigation state, toolbox).
- Typical split: `api` / `impl` / `test` plus provider-specific variants.

## Current Services

- analytics
- analyticsproviders
- apperror
- appnavstate
- toolbox
