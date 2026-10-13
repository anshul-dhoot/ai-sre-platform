# AI-SRE Platform — AI Engineering Instructions

## Engineering Ownership

The human developer is the engineering owner.

AI acts as an implementation and review teammate. AI must not make architectural or product decisions on its own.

## General Rules

* Understand the existing code and requirements before making changes.
* Do not invent requirements.
* Prefer the simplest solution that satisfies the current requirement.
* Do not introduce technologies, dependencies, services, or infrastructure without a clear reason.
* Do not modify unrelated files.
* Preserve existing behavior unless a change is explicitly required.
* Never hard-code secrets, credentials, tokens, or private keys.
* When requirements or architecture are ambiguous, ask before making a significant decision.

## Changes

Before making a significant change:

1. Explain what you intend to change.
2. Identify important assumptions.
3. Highlight architectural or operational implications.

After making changes:

1. Summarize what changed.
2. Identify files changed.
3. Run appropriate validation or tests.
4. Report failures honestly.

## Architecture

Architecture should evolve incrementally.

Do not add distributed-system components merely to demonstrate technology.

Every major component should have a clear purpose and a reason it is needed at the current stage of the project.

## Human Review

The human developer reviews and approves changes before they are committed.

AI-generated code must be treated as a proposal until reviewed and validated.

## Design for simplicity
- Prefer the simplest design that meets the requirements.
- Challenge repeated code, excessive conditionals, unnecessary abstractions, and duplicated logic.
- Consider standard Java and Spring capabilities before introducing a new dependency.
- Do not add dependencies merely to make code shorter.


## Explain code changes
- Explain the purpose of every new class and important configuration file.
- Explain the end-to-end execution flow.
- Explain significant framework defaults and which settings have been explicitly configured.
- Explain important alternatives considered and why the chosen approach fits the current scope.


## Review before completing work
- Inspect existing conventions before coding.
- Review generated code for correctness, readability, maintainability, concurrency and failure behavior.
- Run relevant tests and report actual results.
- Identify assumptions, unverified behavior and deferred improvements.


## Control scope
- Do not implement optional improvements just because they are possible.
- Do not introduce infrastructure or dependencies without explaining the benefit and trade-off.
- Stop and ask when a significant design choice is ambiguous.
- Preserve human approval before commits and significant architectural changes.
