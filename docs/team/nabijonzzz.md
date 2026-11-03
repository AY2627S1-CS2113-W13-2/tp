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

* **Core structure of the app** ([#11](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11)): the abstract
  `Command` class, the `Parser`, `Transaction` and `TransactionList`, `Ui`, `FinNusException` and the main command
  loop, so five developers could add commands in parallel without changing each other's code.
* **`deposit` and `withdraw`** ([#11](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11)): the amount and
  description can be given in any order, and `d/` only counts as a prefix at the start of a word, so descriptions
  such as "and/or" are not misread. One amount check rejects negative numbers, scientific notation, `NaN`, `$` signs
  and more than 2 decimal places. I chose `BigDecimal` over `double` so balances stay exact (later wrapped in `Money`).
* **Spending categories, `t/CATEGORY` and `category`**
  ([#65](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/65)): every withdrawal belongs to one of nine predefined
  categories or a custom one. Unknown categories are rejected, so a typo such as `t/fod` cannot split spending.
  Generalised the currency prefix parsing into one helper shared by `c/`, `t/` and `on/`. Older save files still
  load, and an unknown category in a hand-edited file loads as `Others` with a warning instead of being lost.
* **Transaction dates, `on/YYYY-MM-DD`** ([#73](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/73)): every
  transaction records its day, today by default. Invalid, non-existent (e.g. `2026-02-30`), signed, pre-2000 and
  future dates are rejected, also when loading the save file. Today's date is passed into the parser, so tests never
  depend on the real date. The allowance feature later used the dates to show spending per budget cycle.
* **`find KEYWORD`** ([#71](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/71),
  [#80](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/80)): case-insensitive search using the same numbers as
  `recent`, so results can be deleted directly. Matching compares characters one at a time instead of lowercasing,
  so "İstanbul" is still found.
* **Logging** ([#31](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/31)): log messages go to `finnus.log`, not
  the console, so they never mix with the user's output.

### Contributions to the User Guide

* Wrote the sections for `deposit`, `withdraw`, categories, dates and `find`
  ([#18](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/18),
  [#65](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/65),
  [#71](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/71),
  [#73](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/73)), the FAQ and the Command Summary
  ([#64](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/64)), and kept all output examples accurate.

### Contributions to the Developer Guide

* Wrote "Adding a deposit or withdrawal", "Categorising spending", "Recording transaction dates" and "Finding
  transactions": classes involved, step-by-step processing, save file format and design considerations
  ([#45](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/45),
  [#65](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/65),
  [#71](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/71),
  [#73](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/73)).
* Added a class diagram of the classes involved in categorising spending and a sequence diagram of
  `withdraw 4.50 d/Lunch t/food on/2026-10-20` ([#81](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/81)).

### Contributions to team-based tasks

* Tested the v1.0 and v2.0 release JARs end to end, published v2.0, and reported UG and DG problems before releases.
* Built and tested teammates' pull requests merged with `master` before approving them, which caught e.g. the JAR
  entry point still pointing to the old `Duke` class, a duplicate `getBalance()` that would have broken the build, `recent`
  crashing on an empty list, and a merge that deleted two date constants.
* Wrote the root `README.md` ([#86](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/86)). Before the final
  submission, updated the UG, DG and README to the final behaviour, removed leftover template content, and tested
  the final JAR and every documentation page ([#131](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/131),
  [#134](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/134)).

### Review contributions

* Reviewed teammates' pull requests, e.g.
  [#16](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/16),
  [#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17),
  [#20](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/20),
  [#69](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/69),
  [#76](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/76),
  [#78](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/78),
  [#79](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/79),
  [#83](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/83),
  [#84](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/84),
  [#85](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/85) and
  [#128](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/128).

### Contributions beyond the project team

* Reviewed the Developer Guide of team CS2113-T10-3 (BusyNotFree).
