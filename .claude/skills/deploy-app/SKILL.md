---
name: deploy-app
description: Tag and commit a release of Euclidean Sketchpad. Bumps APP_VERSION, updates CHANGELOG, syncs spec/handoff docs, and creates a git tag. Use when user says "deploy", "release", "cut a release", "deploy-app", or wants to prepare a versioned release.
allowed-tools: Bash(git tag *) Bash(git add *) Bash(git commit *) Bash(git push *) Bash(gh pr create *) Bash(./gradlew *)
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
   - **Requested == current**: check if the git tag `v<version>` already exists (`git tag -l "v<version>"`). If it does, **stop** and tell the user the version is already released — they must provide a higher version number. If the tag does not exist, skip version-bump steps and proceed to doc review and tagging.
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

### 4. Run tests

- Run `./gradlew clean test`.
- If any test fails, **stop the release**. Report the failures to the user and do not proceed to commit or tag.

### 5. Git tag and commit

- Stage all changed files: `git add` each modified file by name.
- Commit with message: `Release v<version>`

### 6. Push and release

Check which branch you are on (`git branch --show-current`).

**If on `main`:**
- Create an annotated git tag: `git tag -a v<version> -m "Release v<version>"`
- Push the commit and tag: `git push --follow-tags`
- Explain that pushing the `v*` tag triggers the GitHub Actions release workflow (`.github/workflows/release.yml`), which builds macOS (.dmg) and Windows (.msi) installers and creates a GitHub Release with them attached.

**If on a feature/side branch:**
- Do **NOT** create the git tag yet.
- Push the branch: `git push -u origin <branch-name>`
- Create a pull request: `gh pr create --title "Release v<version>" --body "<brief description of what changed in this release>"`
- Tell the user: after the PR is merged, switch to `main`, pull, then create and push the tag:
  ```
  git checkout main && git pull
  git tag -a v<version> -m "Release v<version>"
  git push --follow-tags
  ```
- **IMPORTANT — tag must be pushed from `main`.** The release tag must point to a commit on `main` (typically the merge commit) and be pushed from `main`. If the tag is created on a feature branch and pushed before merging, the release workflow may fire against the branch commit. If the branch is later deleted, the release can fail or never trigger.
