# Heng Yong Hen - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students managing a tight monthly budget, including
international students who receive allowances in foreign currencies. Users record deposits and withdrawals, manage
categories and allowances, review their transaction history, and receive warnings before risky spending.

My work focused on first-run setup, spending alerts, transaction ordering and deletion, and defensive command parsing.

## Summary of Contributions

### Code contributed

[RepoSense link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=lololol8484)

### Enhancements implemented

* **First-run setup and persistence** ([#49](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/49)): implemented the
  `Setup` flow for collecting and confirming the opening balance before normal commands begin.
  * The opening balance is applied only after confirmation and is stored separately from transactions, so it cannot
    appear in transaction history or be deleted as income.
  * Added the pending-setup persistence state: an interrupted setup resumes on the next run, while a confirmed balance
    is saved before optional setup questions and allows later runs to skip setup.
  * Integrated optional balance-alert and allowance-percentage prompts with the existing allowance setup, and persisted
    the resulting settings.
* **Spending alerts** ([#110](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/110)): implemented `SpendingAlert`,
  `Confirmation`, and `AlertCommand`.
  * Withdrawals are checked against the optional balance and allowance-percentage thresholds, plus the fixed allowance,
    opening-balance and zero-balance thresholds.
  * A withdrawal that newly crosses one or more thresholds lists all applicable warnings and requires explicit
    confirmation. The same protection covers foreign-currency withdrawals and withdrawal edits.
  * Alert settings are persisted, can be viewed or changed after setup, and retain the other setting when one is
    updated or cleared.
* **Chronological transaction storage** ([#109](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/109)): changed
  `TransactionList` to insert transactions by date while preserving entry order for equal dates.
  * `recent`, `find` and `delete` now use consistent newest-first indices.
  * Backdated transactions are inserted in the correct position, and unsorted save files are normalised in memory when
    loaded while undated legacy entries remain supported.
* **Transaction deletion** ([#21](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/21),
  [#40](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/40)): implemented `delete INDEX`, validating the newest-first
  index shown by `recent` and `find`, removing the matching transaction, and leaving the opening balance untouched.
* **Command parsing and validation** ([#78](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/78),
  [#79](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/79), [#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128)):
  refactored `category` so it lists categories with no argument and adds a category with `category NAME`, while
  keeping `add` and `list` valid names. Improved `c/`, `t/` and date-prefix handling with optional whitespace and
  added pre-extraction checks so one prefix cannot hide a missing value for another prefix.
* **Defensive implementation quality** ([#48](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/48)):
  added assertions, logging and explanatory comments around command execution, setup, alert decisions, parsing,
  persistence and transaction-list invariants. Added reusable confirmation handling so invalid answers and end-of-input
  are handled consistently without mutating the ledger.

### Contributions to the User Guide

* Updated the initial user guide and command examples ([#28](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/28)).
* Documented first-run setup, interrupted setup recovery, optional alert settings, and the fact that the opening balance
  is not a transaction ([#77](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/77)).
* Documented spending-alert behavior, including all five thresholds, allowance-cycle scope, crossing semantics,
  confirmation responses, and the `alert` command ([#110](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/110)).
* Updated category syntax and valid `add` and `list` category names ([#79](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/79)),
  and documented chronological ordering for `recent`, `find` and `delete`, including backdated and legacy undated
  transactions ([#109](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/109)).
* Documented the whitespace and malformed-prefix parser fixes ([#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128)).

### Contributions to the Developer Guide

* Wrote and updated the first-run setup, spending-alert, deletion and chronological transaction sections, including
  persistence formats, control flow, validation rules and design alternatives ([#77](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/77),
  [#109](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/109),
  [#110](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/110)).
* Documented parser validation that checks prefix values before field extraction and the resulting regression tests
  ([#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128)).
* Added and maintained the setup sequence diagram ([#77](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/77),
  [#110](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/110)); [source](../diagrams/SetupSequenceDiagram.puml),
  [rendered image](../images/SetupSequenceDiagram.png).

### Contributions to team-based tasks

* Added the initial JUnit coverage for balance, recent transactions and deletion ([#30](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/30)).
* Added regression and integration tests for setup confirmation and interruption, alert thresholds and persistence,
  chronological insertion and unsorted-file recovery, deletion indices, category parsing, and malformed prefixes
  ([#49](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/49), [#79](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/79),
  [#109](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/109), [#110](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/110),
  [#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128)).
* Added the initial team-member documentation entry in `AboutUs.md` ([#2](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/2)).

### Review/mentoring contributions

* Reviewed the category-search feature ([#103](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/103)), checking its
  parser, command, documentation and test changes and suggesting clearer consistency for search totals and future
  filtering extensions.
* Reviewed the validation-regex refactor ([#125](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/125)), checking
  shared validation behavior across setup, parsing, storage and help-related tests.
* Reviewed the negative-amount validation change and its parser regression tests
  ([#102](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/102)).
* Reviewed substantial documentation contributions, including the User Guide update ([#27](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/27)),
  the Developer Guide sections on opening balances and exchange rates
  ([#47](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/47)), and a teammate's PPP
  ([#91](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/91)).
* Reviewed cross-cutting code-quality changes for logger standardisation
  ([#52](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/52)) and a recent-command crash fix with regression tests
  ([#29](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/29)).

### Contributions beyond the project team

No beyond-team contributions are included in this version.
