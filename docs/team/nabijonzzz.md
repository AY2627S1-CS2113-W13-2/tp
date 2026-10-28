# Nabijon Nazarov - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including
international students who receive allowances in foreign currencies. Users record deposits and withdrawals,
view their balance and recent transactions, and set exchange rates, all by typing short commands.
It is written in Java by a team of 5.

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
* **Logging:** made the app write all log messages to `finnus.log` instead of the console, so they never mix with
  the output the user sees. If the log file cannot be created, the app keeps running without logging.

### Contributions to the User Guide

* Wrote the sections for `deposit` and `withdraw`.
* Added the FAQ answers and completed the Command Summary.

### Contributions to the Developer Guide

* Wrote the "Adding a deposit or withdrawal" section: classes involved, step-by-step processing of a command,
  validation rules and design considerations (`BigDecimal` vs `double`, required vs optional description).

### Contributions to team-based tasks

* Reviewed most of the team's pull requests for v1.0 and found several problems before release, e.g. the JAR
  entry point still pointing to the old `Duke` class after the package was renamed, a duplicate `getBalance()`
  method that would have broken the build, and `recent` crashing on an empty transaction list.
* Tested the v1.0 release JAR end to end before and after publishing it.

### Review contributions

* Reviewed the Developer Guide of team CS2113-T10-3 (BusyNotFree).
