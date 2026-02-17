导航: `../CLAUDE.md` > `upstream_infra/`

# upstream_infra/ (upstream CI workflows and infrastructure)

## Responsibility

- Holds GitHub Actions workflows and infra shared with upstream Element X.
- Note: repo root `.github/workflows/` only contains housekeeping (`stale.yml`).

## Key Paths

- Workflows: `upstream_infra/.github/workflows/`
  - Quality: `upstream_infra/.github/workflows/quality.yml`
  - Tests: `upstream_infra/.github/workflows/tests.yml`
  - Build: `upstream_infra/.github/workflows/build.yml`
  - Danger: `upstream_infra/.github/workflows/danger.yml`
