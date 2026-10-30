# Heng Yong Hen - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including international
students who receive allowances in foreign currencies. Users record deposits and withdrawals, view their balance and
transaction history, and manage their spending with short commands.

My work focused on first-run setup, transaction deletion, and improving command parsing and input validation.

## Summary of Contributions

### Code contributed

[RepoSense link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=lololol8484)

### Enhancements implemented

* **First-run setup** ([#49](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/49)): implemented the `Setup` flow for
  collecting and confirming the user's opening balance before normal commands begin.
  * The opening balance is applied only after confirmation and remains separate from transactions, so it cannot appear
    in transaction history or be deleted as income.
  * Added persistence handling for incomplete setup: a pending marker causes setup to resume after an interrupted run,
    while a confirmed balance lets later runs skip setup.
* **Transaction deletion** ([#21](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/21)): implemented `delete INDEX`,
  using the newest-first indices shown by `recent` and `find`. The command validates the index, removes the matching
  transaction, reports the deleted record, and leaves the opening balance untouched.
* **Category command parsing** ([#79](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/79)): refactored category input
  so `category` lists categories and `category NAME` adds one. Names such as `add` and `list` remain valid category
  names. Also updated parsing of `c/` and `t/` values to accept a space after the prefix, e.g. `c/ JPY` and `t/ food`.
* **Assertions and logging:** added invariant checks and diagnostic logging across command execution, parsing, and
  persistence code, including the delete command. These checks help catch invalid internal state while logs record
  useful details without changing the normal command output.

### Contributions to the User Guide

* Updated first-run setup instructions to explain confirmation, interrupted setup, and when normal commands become
  available.
* Updated the category instructions and command summary to match the current syntax.

### Contributions to the Developer Guide

* Wrote the first-run setup and transaction deletion sections, including their control flow and design decisions.
* Added the setup sequence diagram ([source](../diagrams/SetupSequenceDiagram.puml),
  [rendered image](../images/SetupSequenceDiagram.png)).
* Updated category command descriptions to match the refactored parser ([#77](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/77)).

### Contributions to team-based tasks

* Added tests for setup confirmation, interrupted setup, deletion behavior, and category parsing to protect these flows
  as the project features were integrated.

### Review/mentoring contributions

No review or mentoring contributions are included in this first version.

### Contributions beyond the project team

No beyond-team contributions are included in this first version.
