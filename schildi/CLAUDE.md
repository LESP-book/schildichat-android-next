导航: `../CLAUDE.md` > `schildi/`

# schildi/* (SchildiChat-specific additions)

## Responsibility

- Downstream-only modules for SchildiChat features and overrides.

## Directories

- `schildi/lib`: Schildi-specific utilities and strings (preferred for downstream code that should not depend on Element modules).
- `schildi/components`: Schildi components that depend on Element UI/designsystem but little else.
- `schildi/theme`: Schildi theme tweaks.
- `schildi/matrixcore`, `schildi/matrixsdk`: Schildi forks/wrappers around Matrix-related layers.
- `schildi/screenshots`: screenshot tooling/assets.

## Downstream Conventions

- Prefer adding new code in `chat.schildi.*` packages to reduce merge conflicts.
- Avoid editing upstream strings; use Schildi modules/flavors instead.

Reference: `readme.md` (Contributing section).
