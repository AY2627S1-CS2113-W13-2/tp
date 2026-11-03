<!-- @@author naieel04 -->

# Ian Lee - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students, including those receiving allowances in foreign
currencies. It helps users record income and spending, review transactions, and manage exchange rates.

My work focused on saving and recovering financial records, transaction editing, and keeping command help and
documentation consistent with the application.

## Summary of Contributions

### Code contributed

[RepoSense dashboard](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=naieel04&breakdown=true)

### Enhancements implemented

* **Automatic saving and recovery** ([#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43)):
  Users can resume their financial records across application sessions.
  * Implemented the initial persistence subsystem, including a versioned ledger format, encoding and decoding,
    startup restoration, and automatic saving after changes.
  * Recovers usable records from damaged files, attempts backup recovery, and preserves damaged originals before
    replacement.
  * Uses temporary files and atomic replacement to reduce the risk of leaving an incomplete main file.


* **Transaction editing** ([#126](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/126)):
  Users can correct deposits and withdrawals without deleting and recreating entries.
  * Added field-based editing of amounts, descriptions, withdrawal categories, and dates using the transaction
    numbers shown in transaction lists.
  * Foreign-currency edits use the transaction's saved exchange rate or an explicitly supplied correction,
    preserving historical values independently of current global rates.
  * Validates the full replacement before changing the ledger and requires confirmation for edits that cross
    spending-alert thresholds.


* **Allowance and setup reliability** ([#126](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/126)):
  fixed transaction numbering and opening-balance persistence during allowance and setup operations.
  * Prevents automatic allowance deposits from shifting transaction numbers after a list was displayed.
    Due allowances are credited before most commands, while `edit` and `delete` consume the displayed index before
    crediting allowances.
  * Saves the confirmed opening balance before optional setup questions, so it survives an interruption after
    a successful save.


* **Help command and shared command metadata**:
  users can look up command names, descriptions, and syntax in a bordered table.
  * Implemented the help table ([#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17)), then added assertions
    and logging ([#36](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/36)).
  * Introduced shared command metadata used by the parser and help table to reduce duplicated command names and
    usage instructions ([#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43)).
  * Constrained the table to 80 columns and added wrapping so long syntax remains readable in standard terminals
    ([#84](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/84)).

### Contributions to the User Guide

* **Help and command documentation** ([#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17)):
  * Added the help-command explanation.
  * Documented available command names, descriptions, and syntax.
* **Saving and recovering data** ([#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43)):
  * Explained automatic saving across application sessions.
  * Documented damaged ledger rows and files, and backup recovery.
* **Transaction editing and guide accuracy** ([#126](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/126)):
  * Documented edit syntax, SGD and foreign-currency examples, validation, and spending-alert confirmation.
  * Updated setup-saving explanations, transaction ordering, and log-file location to match application behaviour.
  * Updated the complete help-table example to match console output.

### Contributions to the Developer Guide

* **System design documentation** ([#76](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/76)):
  * Documented architecture, storage, and automatic saving and recovery, including component roles, file format,
    change detection, backups, and failure handling.
  * Added architecture, class, sequence, and recovery activity diagrams with editable PlantUML sources.
* **Guide structure and initial requirements** ([#69](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/69)):
  * Reorganised the guide into clearer sections while preserving existing content and author attribution.
  * Added initial user stories and non-functional requirements.
* **Edit design and integration** ([#126](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/126)):
  * Documented the edit workflow, historical FX corrections, validation, confirmation, and cancellation.
  * Added an edit sequence diagram.
  * Corrected storage field counts and explanations of allowance timing, opening-balance saving, and transaction
    ordering.

### Contributions to team-based tasks

- Wrote unit and integration tests for ledger encoding and decoding, save failures, corruption recovery, and
  persistence across application restarts ([#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43)). Added
  help-command tests for complete output, aligned borders, the 80-column limit, wrapping, and preservation of existing
  transactions ([#84](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/84)).
- Added regression tests for SGD and foreign-currency edits, persistence of edited records, spending-alert approval
  and cancellation, allowance timing across command execution, and interrupted optional setup
  ([#126](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/126)).
- Configured Gradle to launch FinNUS and package its executable JAR:
  [build configuration](https://github.com/naieel04/tp/commit/6f437b6).
- Updated the project introduction in the documentation README
  ([#85](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/85)).

### Review/Mentoring contributions

* Reviewed 22 PRs: [#2](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/2),
[#11](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11),
[#15](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/15),
[#16](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/16),
[#20](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/20),
[#31](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/31),
[#35](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/35),
[#39](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/39),
[#40](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/40),
[#45](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/45),
[#65](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/65),
[#71](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/71),
[#81](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/81),
[#87](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/87),
[#89](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/89),
[#90](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/90),
[#101](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/101),
[#112](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/112),
[#115](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/115),
[#118](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/118),
[#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128),
[#131](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/131).

Examples of review feedback:

- In [#115](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/115#discussion_r4443256088), flagged that cash-flow
  charts could miss newly due allowances. Also suggested reusing shared command syntax instead of duplicating it
  in the parser ([comment](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/115#discussion_r4443320720)).
- In [#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128#pullrequestreview-5746131968), identified that
  removing fields before validating empty prefixes could accept malformed commands, and suggested checking the
  original arguments first.
- In [#112](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/112#discussion_r4443000541), suggested clarifying
  that the maximum amount is measured in SGD to avoid confusion for users of high-denomination currencies.
- In [#71](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/71#discussion_r4410020156), identified missed Unicode
  search matches and suggested `regionMatches(true, ...)`.
- In [#11](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11#discussion_r4203266440), suggested `BigDecimal` for
  monetary precision.
- In [#35](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/35#discussion_r4260591341), flagged inconsistent monetary
  formatting in logs and suggested `formatMoney()`.

### Contributions beyond the project team

- Reviewed the Developer Guide of [ScheduleFlow PR](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/29) [CS2113-T10-1].

<!-- @@author -->
