---
name: deploy-app
description: Tag and commit a release of Euclidean Sketchpad. Bumps APP_VERSION, updates CHANGELOG, syncs spec/handoff docs, and creates a git tag. Use when user says "deploy", "release", "cut a release", "deploy-app", or wants to prepare a versioned release.
allowed-tools: Bash(git tag *) Bash(git add *) Bash(git commit *)
---

# Deploy App

Prepare a versioned release: bump version, update docs, tag, and commit.

## Inputs

This skill requires a **version number** (e.g. `1.0.1`, `1.1.0`, `2.0.0`).

- If the user did not provide a version number, **ask for one before proceeding**.
- The version follows `<major>.<minor>.<patch>` format, no `v` prefix in code (the git tag uses `v` prefix).

## Version validation

1. Read the current version from `APP_VERSION` in `src/main/kotlin/euclideansketchpad/Main.kt`.
2. Compare the requested version to the current version using semver ordering:
   - **Requested < current**: warn the user that this is a downgrade and ask for confirmation before proceeding.
   - **Requested == current**: skip version-bump steps (no code changes needed). Proceed to doc review and tagging only if there are untagged changes.
   - **Requested > current**: proceed with the full flow below.

## Release flow (version > current)

### 1. Bump APP_VERSION

Edit `src/main/kotlin/euclideansketchpad/Main.kt` — change the `APP_VERSION` constant to the new version string (no `v` prefix).

### 2. Update CHANGELOG.md

- Read `CHANGELOG.md`.
- Add a new `## [<version>] - <today's date YYYY-MM-DD>` section **above** the previous version entry (reverse chronological order).
- Populate it with what changed since the last release. Determine changes by:
  - Reading `git log` from the last version tag to HEAD for committed changes.
  - Running `git diff` and `git status` to capture uncommitted and unstaged local changes.
  - Grouping under `### Added`, `### Changed`, `### Fixed`, or `### Removed` subsections as appropriate (omit empty subsections).
- Do NOT alter existing version entries.

### 3. Review spec and handoff

- Read `spec/SPEC.md` and `handoff/HANDOFF.md`.
- Check whether any recent implementation changes have caused the spec or handoff to become stale. Focus on:
  - New features or behavior not yet documented in the spec.
  - Spec sections that describe behavior differently from the current implementation.
  - Handoff sections with outdated build state, test counts, or architecture notes.
- **No absolute paths.** Every path written to tracked files must be relative to the project root. Never write absolute filesystem paths (e.g. `/Users/...` or `/home/...`) — they leak private machine details and break on other machines.
- If updates are needed, make them. If everything is current, skip.

### 4. Git tag and commit

- Stage all changed files: `git add` each modified file by name.
- Commit with message: `Release v<version>`
- Create an annotated git tag: `git tag -a v<version> -m "Release v<version>"`
- **Remind the user that the commit and tag have NOT been pushed.** Suggest: `git push --follow-tags`
- Explain that pushing the `v*` tag will trigger the GitHub Actions release workflow (`.github/workflows/release.yml`), which builds macOS (.dmg) and Windows (.msi) installers and creates a GitHub Release with them attached.
- **IMPORTANT — tag must be pushed from `main`.** The release tag must point to a commit on `main` (typically the merge commit) and be pushed from `main`. If the tag is created on a feature branch and pushed before merging, the release workflow may fire against the branch commit. If the branch is later deleted, the release can fail or never trigger. Always: merge the PR first, switch to `main`, pull, then tag and push.
