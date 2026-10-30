# Nabijon Nazarov - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including
international students who receive allowances in foreign currencies. Users record deposits and withdrawals,
tag spending with categories, search and review their transactions, set exchange rates and track their monthly
allowance, all by typing short commands. It is written in Java by a team of 5.

## Summary of Contributions

### Code contributed

[RepoSense link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=nabijonzzz)

### Enhancements implemented

* **Core structure of the app:** set up the structure that every command is built on: the abstract `Command`
  class, the `Parser` that turns user input into commands, `Transaction` and `TransactionList` as the single
  place where transactions are stored, `Ui`, `FinNusException` and the main command loop.
  * Justification: the repo started as an empty template, so a shared structure let five developers add their
    commands in parallel without changing each other's code.
* **`deposit` and `withdraw` commands:** record money coming in and going out.
  * The amount and the description can be given in any order, and `d/` only counts as a prefix at the start of a
    word, so descriptions such as "and/or" are not misread.
  * Amounts are validated with a single check that rejects negative numbers, scientific notation, `NaN`, `$` signs
    and more than 2 decimal places, and are stored as `BigDecimal` so balances stay exact.
* **Spending categories (`t/CATEGORY`, `category add` / `category list`):** every withdrawal belongs to one
  category, either one of nine predefined ones or a custom one, so users can see where their money goes.
  * Unknown categories are rejected instead of being created, so a typo such as `t/fod` cannot split the user's
    spending into a new category.
  * Generalised the currency prefix parsing into one shared helper used by `c/`, `t/` and `on/`, instead of
    copying it for each prefix.
  * Extended the save file format so categories are saved, while files from older versions still load: their
    withdrawals become `Others`, and an unknown category in a hand-edited file loads as `Others` with a warning
    instead of losing the transaction.
* **Transaction dates (`on/YYYY-MM-DD`):** every transaction records the day it happened, today by default.
  * Rejects invalid, non-existent (e.g. `2026-02-30`), signed (e.g. `-2026-01-01`), pre-2000 and future dates,
    with the same rules applied when loading the save file.
  * Today's date is passed into the parser, so the tests never depend on the real date.
  * The dates made it possible for the monthly allowance feature to show spending per budget cycle.
* **`find KEYWORD`:** searches transaction descriptions, ignoring case. Results use the same numbers as
  `recent`, so a user can `delete` a transaction straight from the results.
  * Matching compares one character at a time instead of lowercasing, so text such as "İstanbul" is still found.
* **Logging:** made the app write all log messages to `finnus.log` instead of the console, so they never mix with
  the output the user sees. If the log file cannot be created, the app keeps running without logging.

### Contributions to the User Guide

* Wrote the sections for `deposit`, `withdraw`, categorising spending (`t/`, `category`), recording dates (`on/`)
  and `find`.
* Added the FAQ answers and completed the Command Summary.
* Kept every transaction output example in the UG accurate when categories and dates changed the output.

### Contributions to the Developer Guide

* Wrote the sections "Adding a deposit or withdrawal", "Categorising spending", "Recording transaction dates" and
  "Finding transactions": classes involved, step-by-step processing, save file format and design considerations.
* Added UML diagrams:
  * a class diagram of the classes involved in categorising spending;
  * a sequence diagram of a withdrawal with a category and a date (`withdraw 4.50 d/Lunch t/food on/2026-10-20`).

### Contributions to team-based tasks

* Tested the v1.0 and v2.0 release JARs end to end before and after publishing them, and published the v2.0 release.
* Checked that teammates' pull requests build and pass all tests when merged with `master` before approving them,
  which caught several problems before they reached `master`, e.g.:
  * the JAR entry point still pointing to the old `Duke` class after the package was renamed;
  * a duplicate `getBalance()` method that would have broken the build;
  * `recent` crashing on an empty transaction list;
  * a merge that deleted two constants used by dates, which would have stopped the app from compiling.
* Reviewed and fixed the UG and DG before releases, e.g. removing descriptions of features the app did not have.

### Review contributions

* Reviewed teammates' pull requests, e.g.
  [#16](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/16),
  [#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17),
  [#20](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/20),
  [#69](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/69),
  [#76](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/76),
  [#78](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/78),
  [#79](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/79),
  [#83](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/83) and
  [#84](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/84).
* Reviewed the Developer Guide of team CS2113-T10-3 (BusyNotFree).
