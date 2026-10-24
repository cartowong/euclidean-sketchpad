---
name: save-changes
description: Update the handoff document, spec, and changelog to reflect this session's work, then commit everything to git.
allowed-tools: Bash(git add *) Bash(git commit *) Bash(git diff *) Bash(git log *) Bash(git status *) Bash(./gradlew clean test)
---

# Save Changes

Update documentation to reflect this session's work and commit everything.

## Principles

- **No absolute paths.** Every path in tracked files must be relative to the project root. Never write absolute filesystem paths (e.g. `/Users/...` or `/home/...`).
- **Minimal, accurate updates.** Only change what this session's work requires. Don't rewrite sections that are already correct.

## Instructions

### 1. Gather context

- Read `handoff/HANDOFF.md`, `spec/SPEC.md`, and `CHANGELOG.md`.
- Run `git diff` and `git status` to see what changed this session.
- Run `git log` from the last version tag to HEAD to understand recent committed changes.

### 2. Update `spec/SPEC.md`

The spec is the **authoritative design reference**. Update it if this session's work changed behavior that the spec describes:

- New features or object types not yet documented.
- Sections that describe behavior differently from the current implementation.
- New or changed hit-testing tiers, tool behaviors, menu items, rendering passes, etc.

If the spec is already accurate, skip this step.

### 3. Update `CHANGELOG.md`

Add entries under the **current unreleased version section** (the topmost `## [x.y.z]` entry) for work done this session. If no unreleased section exists yet, create one with today's date above the previous release.

- Group under `### Added`, `### Changed`, `### Fixed`, or `### Removed` as appropriate (omit empty subsections).
- Do NOT alter existing released version entries.
- Keep entries concise — one line per change.

### 4. Update `handoff/HANDOFF.md`

The handoff is a **concise onboarding brief**, not a changelog. It covers only what the spec doesn't: current build state, implementation-specific architecture decisions, and known issues.

- `## Current State` — update build status, test count, branch, packaging version.
- `## Architecture Notes` — add notes for new implementation patterns or gotchas introduced this session. Remove notes that are no longer accurate. Do NOT duplicate information already in the spec.
- `## Known Issues / Gaps` — remove fixed items, add newly discovered items.

Do NOT add session-by-session changelogs. Completed work belongs in git history and `CHANGELOG.md`.

### 5. Run tests

- Run `./gradlew clean test`.
- If any test fails, **stop**. Report the failures and do not commit.

### 6. Stage and commit

- `git add` all files changed during this session (implementation files, doc files, skill files, etc.) by name. Do not use `git add -A` or `git add .`.
- Commit with a message summarizing the session's work. Format:
  ```
  <short summary of implementation changes>

  Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
  ```
