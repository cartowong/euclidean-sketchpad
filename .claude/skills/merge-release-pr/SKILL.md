---
name: merge-release-pr
description: Merge an existing release PR with admin privileges, push the release tag from main, and optionally clean up the release branch. Use when user says "merge release", "merge release pr", "merge-release-pr", or wants to complete a release after the PR was created.
allowed-tools: Bash(gh pr *) Bash(git checkout *) Bash(git pull *) Bash(git push *) Bash(git tag *) Bash(git branch *)
---

# Merge Release PR

Merge a release PR, push the release tag from `main`, and clean up the release branch.

This skill is the second half of the release workflow — it picks up after `deploy-app` has created a release branch, committed the version bump, and opened a PR.

## Instructions

### 1. Find the release PR

Run `gh pr list --head "release/" --state open` to find open PRs from `release/*` branches.

- If **no release PR is found**, tell the user there is no open release PR and stop. Do not proceed.
- If **multiple release PRs are found**, list them and ask the user which one to merge.
- If **exactly one** is found, confirm it with the user (show the PR number, title, and branch name) before proceeding.

### 2. Merge the PR

Merge with admin override to bypass branch protection rules:

```
gh pr merge <number> --merge --admin
```

- If the merge fails, report the error and stop.

### 3. Switch to main and pull

```
git checkout main
git pull
```

### 4. Create and push the release tag

Extract the version number from the PR title (expected format: `Release v<version>`). Then:

```
git tag -a v<version> -m "Release v<version>"
git push --follow-tags
```

Explain to the user that pushing the `v*` tag triggers the GitHub Actions release workflow (`.github/workflows/release.yml`), which builds macOS (.dmg) and Windows (.msi) installers and creates a GitHub Release with them attached.

### 5. Clean up the release branch

**Ask the user for confirmation** before deleting the release branch. Show the branch name and ask if they want it deleted.

If the user confirms:

```
git branch -d release/<version>
git push origin --delete release/<version>
```

If the user declines, skip this step.
