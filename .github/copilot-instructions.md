# GitHub Copilot instructions

This repository's primary agent guidance lives in [`AGENTS.md`](../AGENTS.md)
at the project root. Read it first — it covers the project overview, tech
stack, repository layout, build/test/run commands, and conventions.

Everything in `AGENTS.md` applies here. This file only adds Copilot-specific
notes.

## Copilot-specific notes

- When generating or modifying code, follow the package structure and
  conventions described in `AGENTS.md`.
- Prefer targeted Gradle commands (e.g. `./gradlew test --tests "..."`) over
  the full `./gradlew build` when validating small changes.
- Do not run `ktlintCheck` (or any ktlint Gradle task) in this repository.
  Skip ktlint validation entirely.
