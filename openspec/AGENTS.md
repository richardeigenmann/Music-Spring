# OpenSpec Agent Instructions

This project uses the **OpenSpec** framework for spec-driven development. 

## How to use OpenSpec
1. **Propose Changes**: All significant features or fixes MUST start with a proposal in `openspec/changes/<change-id>/proposal.md`.
2. **Design First**: Before implementing, create a `design.md` and `specs.md` (delta specs) in the change directory.
3. **Task Tracking**: Use `tasks.md` to track implementation progress.
4. **Update Specs**: Once a change is complete, its delta specs SHOULD be merged into the main `openspec/specs/` library and the change folder archived.

## Workflow Commands
- `/opsx:propose "feature description"`: Start a new change.
- `/opsx:design`: Document the technical approach.
- `/opsx:implement`: Begin code changes following the tasks.
- `/opsx:archive`: Wrap up a completed change.

## Project Knowledge
Refer to `openspec/project.md` for technical stack and architectural details.
Refer to `openspec/specs/` for the current system behavior definitions.
