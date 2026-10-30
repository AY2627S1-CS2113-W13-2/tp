# Ian Lee - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students, including those receiving allowances in foreign
currencies. It helps users record income and spending, review transactions, and manage exchange rates.

## Summary of Contributions

### Code contributed

[RepoSense dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=naieel04&breakdown=true)

### Enhancements implemented

- **Automatic saving and recovery**: Implemented the initial persistence subsystem, including the versioned ledger
format, encoding and decoding, startup restoration, and automatic saving after changes. This allows users to resume
their financial records across application sessions.


- [#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43) **Persistence implementation:** The implementation handles incomplete or damaged files, attempts recovery from backups, and preserves damaged
originals before replacement. Saving uses temporary files and atomic replacement to reduce the risk of leaving an
incomplete main file. Integrating recovery and saving with the command loop required coordinating stored state,
application state, and failure handling.


- [#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17),
[#36](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/36)  **Help command and shared command metadata** - Implemented the help command and introduced shared command metadata
used by the parser and help table. This reduces duplication when command names and usage instructions change.
Subsequently constrained the table to 80 columns and added wrapping so long syntax remains readable in standard
terminals.


### Contributions to the User Guide

- [#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17) **Help command and command documentation:** Added the help-command explanation and documented the available commands so users can find command names, descriptions, and syntax.
- **Saving and recovering data:** Documented automatic saving and user expectations when ledger rows or the full data file are corrupted, including backup recovery behavior.

### Contributions to the Developer Guide

- [#76](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/76) **System design documentation:** Documented architecture, storage, and automatic saving and recovery, including component roles, file format, change detection, backups, and failure handling. Added UML diagrams to explain these designs.
- [#69](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/69) **Project documentation:** Added setup, scope, requirements, glossary, manual testing, and planned enhancement sections.

### Contributions to team-based tasks

* Wrote unit and integration tests for ledger encoding and decoding, save failures, corruption recovery, and persistence
  across application restarts. Added help-command tests for complete output, aligned borders, the 80-column limit,
  wrapping, and preservation of existing transactions.
* Configured Gradle to launch FinNUS and package its executable JAR: [Build configuration](https://github.com/naieel04/tp/commit/6f437b6).

### Review/Mentoring contributions
