---
name: save-handoff
description: Update the handoff document in-place and commit it to git.
allowed-tools: Bash(git add *) Bash(git commit *)
---

# Save Handoff

Update the handoff document and commit it.

## Principles

The handoff document is a **concise onboarding brief**, not a changelog. Keep it short so it loads quickly into context. Do NOT duplicate information already in `spec/SPEC.md` — the spec is the authoritative design reference and is already loaded via `CLAUDE.md`. The handoff covers only what the spec doesn't: current build state, implementation-specific architecture decisions, deviations from spec, and known issues.

## Instructions

1. Read `handoff/HANDOFF.md` to understand the current contents.

2. **Update — do not accumulate.** The handoff has these sections:
   - `## Current State` — build status, git branch, test count. Update in place.
   - `## Key Reference Files` — pointers to spec, handoff, guidelines.
   - `## Architecture Notes` — implementation decisions that a new session needs to know (e.g., "colors are programmatic not CSS", "hover is canvas-level"). Add new notes, remove notes that are no longer accurate. Do NOT include information that is already in the spec.
   - `## Known Issues / Gaps` — things that are broken or unimplemented. Remove items that were fixed this session. Add new items discovered this session.

3. **Do NOT add session-by-session changelogs.** Completed work belongs in git history, not in the handoff. If a session's work introduced a new architectural pattern or gotcha, add it to Architecture Notes. If it fixed a known issue, remove that issue. That's it.

4. **Do NOT duplicate the spec.** The spec describes the intended design. The handoff describes only:
   - How the current implementation deviates from or extends the spec
   - Implementation-specific gotchas (e.g., JavaFX CSS vs programmatic colors)
   - What's not yet done (Known Issues / Gaps)

5. **Stage and commit**:
   - `git add handoff/HANDOFF.md`
   - Also `git add` any other files that changed as part of this session's work if they haven't been committed yet.
   - Commit with message: `Update handoff: <short description of what changed>`
