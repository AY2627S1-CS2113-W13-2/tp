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


- ([#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43)) **Persistence implementation:** The implementation handles incomplete or damaged files, attempts recovery from backups, and preserves damaged
originals before replacement. Saving uses temporary files and atomic replacement to reduce the risk of leaving an
incomplete main file. Integrating recovery and saving with the command loop required coordinating stored state,
application state, and failure handling.


- ([#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17),
[#36](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/36))  **Help command and shared command metadata** - Implemented the help command and introduced shared command metadata
used by the parser and help table. This reduces duplication when command names and usage instructions change.
Subsequently constrained the table to 80 columns and added wrapping so long syntax remains readable in standard
terminals.


### Contributions to the User Guide

- ([#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17)) **Help command and command documentation:** Added the help-command explanation and documented the available commands so users can find command names, descriptions, and syntax.
- **Saving and recovering data:** Documented automatic saving and user expectations when ledger rows or the full data file are corrupted, including backup recovery behavior.

### Contributions to the Developer Guide

- ([Architecture](../DeveloperGuide.md#architecture), [Storage](../DeveloperGuide.md#storage)) **Architecture and storage design:** Documented the application architecture and storage components, including file format, snapshots, backups, and recovery behavior.
- ([Automatic saving and recovery](../DeveloperGuide.md#automatic-saving-and-recovery)) **Automatic saving and recovery:** Explained change detection, save retries, backup rotation, corruption recovery, and failure handling, with sequence and activity diagrams.
- ([Documentation, logging, testing, and DevOps](../DeveloperGuide.md#documentation-logging-testing-and-devops)) **Developer setup and documentation:** Added project setup and guide material for documentation, testing, and DevOps.
- ([Product scope](../DeveloperGuide.md#product-scope), [user stories](../DeveloperGuide.md#user-stories), [use cases](../DeveloperGuide.md#use-cases), [non-functional requirements](../DeveloperGuide.md#non-functional-requirements), [glossary](../DeveloperGuide.md#glossary), [manual testing instructions](../DeveloperGuide.md#instructions-for-manual-testing), [planned enhancements](../DeveloperGuide.md#planned-enhancements)) **Project reference sections:** Added the project's product and development reference sections.

[Developer Guide](../DeveloperGuide.md) |
[Diagram additions](https://github.com/naieel04/tp/commit/6d02f6f) |
[Command class diagram](https://github.com/naieel04/tp/commit/51d74fc)

### Contributions to team-based tasks

* Wrote unit and integration tests for ledger encoding and decoding, save failures, corruption recovery, and persistence
  across application restarts. Added help-command tests for complete output, aligned borders, the 80-column limit,
  wrapping, and preservation of existing transactions.
* Configured Gradle to launch FinNUS and package its executable JAR: [Build configuration](https://github.com/naieel04/tp/commit/6f437b6).

### Review/Mentoring contributions
