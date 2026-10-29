# Ian Lee - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students, including those receiving allowances in foreign
currencies. It helps users record income and spending, review transactions, and manage exchange rates.

## Summary of Contributions

**Code contributed:** [RepoSense dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=naieel04&breakdown=true)

### Enhancements implemented

**Automatic saving and recovery** - Implemented the initial persistence subsystem, including the versioned ledger
format, encoding and decoding, startup restoration, and automatic saving after changes. This allows users to resume
their financial records across application sessions.

The implementation handles incomplete or damaged files, attempts recovery from backups, and preserves damaged
originals before replacement. Saving uses temporary files and atomic replacement to reduce the risk of leaving an
incomplete main file. Integrating recovery and saving with the command loop required coordinating stored state,
application state, and failure handling.
[Persistence implementation: PR #43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43)

**Help command and shared command metadata** - Implemented the help command and introduced shared command metadata
used by the parser and help table. This reduces duplication when command names and usage instructions change.
Subsequently constrained the table to 80 columns and added wrapping so long syntax remains readable in standard
terminals.
[Initial help command: PR #17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17) |
[Metadata refactoring: PR #36](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/36) |
[Table wrapping](https://github.com/naieel04/tp/commit/ff00a50)

### Testing

Wrote unit and integration tests for ledger encoding and decoding, save failures, corruption recovery, and persistence
across application restarts. Added help-command tests for complete output, aligned borders, the 80-column limit,
wrapping, and preservation of existing transactions.

### Documentation

- **User Guide:** Wrote the saving and recovery instructions and the help-command explanation.
- **Developer Guide:** Wrote architecture, application lifecycle, storage, automatic saving and recovery, and validation
  explanations. Reorganised the guide and integrated teammates' feature documentation.
- **UML diagrams:** Added six diagrams covering architecture, command execution, command/model relationships, storage
  structure, automatic saving, and storage recovery.

[Developer Guide](../DeveloperGuide.md) |
[Diagram additions](https://github.com/naieel04/tp/commit/6d02f6f) |
[Command class diagram](https://github.com/naieel04/tp/commit/51d74fc)

### Project support

Configured Gradle to launch FinNUS and package its executable JAR.
[Build configuration](https://github.com/naieel04/tp/commit/6f437b6)
