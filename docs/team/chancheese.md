# Chan Chee Heng - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including international
students who receive allowances in foreign currencies. Users record deposits and withdrawals, tag spending with
categories, track their monthly allowance cycle and see how much money they have left, all by typing short commands.

My work focused on the income and currency features: the opening balance, exchange rates, foreign-currency
transactions and the monthly allowance, plus finding spending by category.

## Summary of Contributions

### Code contributed

[RepoSense link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=chancheese)

### Enhancements implemented

* **Monthly allowance (`allowance`)** ([#83](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/83)):
  users set the day their allowance arrives, and optionally its amount and currency.
  * Splits time into budget cycles (e.g. 5 Oct to 4 Nov) and shows a summary of the current cycle:
    a countdown to the next allowance, and the amount received and spent this cycle.
  * With an amount, due allowances are added as dated deposits at startup and after each command. Crediting
    starts from the *next* allowance date, so a deposit the user already logged by hand is never doubled.
  * Complete end to end: a first-run setup prompt, a countdown in `balance`, foreign-currency allowances, and
    saving and loading the setting.
  * Hard parts: date edge cases (e.g. day 31 in a 30-day month, missed months while the app was closed),
    and changing teammates' `Setup`, `BalanceCommand` and `Transaction` without breaking their tests.
* **Foreign-currency transactions (`c/CURRENCY`)** ([#55](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/55)):
  `withdraw 1100 d/Ramen c/JPY` converts to SGD using the stored rate.
  * The rate used is saved with each transaction, so changing a rate later does not change past records.
  * The save format was extended with 3 extra fields while still loading old save files. A row whose SGD
    amount does not match its foreign amount is skipped as corrupted.
  * Added `ForeignAmount` and `ForeignTransactionCommand`, so `DepositCommand` and `WithdrawCommand` did not need to change.
* **Finding spending by category** ([#103](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/103)):
  extended `find` to `find [KEYWORD] [t/CATEGORY]`. A category search lists only that category's withdrawals with
  their SGD total, e.g. how much was spent on food. The category is checked the same way as in `withdraw`.
* **Exchange rates (`rate`)** ([#23](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/23)): rates are entered by hand so the
  app works offline. Currency codes are validated against `java.util.Currency`.
* **Opening balance** ([#22](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/22)): a starting balance kept
  separate from the transaction list, so it is not affected by `recent` or `delete`. Later reused by the team's
  first-run setup.
* **Clearer errors:** negative amounts in `deposit`, `withdraw`, `c/`, `allowance` and setup now get their own
  message ([#102](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/102)).

### Contributions to the User Guide

* Wrote the sections on first-run setup, exchange rates, recording an amount in another currency (`c/`) and
  setting a monthly allowance, and the category search in `find`
  ([#55](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/55), [#59](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/59),
  [#83](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/83), [#103](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/103)).
* Kept examples in line with the real output (`help` table, `balance`, `history` ordering) and fixed issues from
  peer review ([#101](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/101),
  [#118](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/118)).

### Contributions to the Developer Guide

* Wrote *Setting exchange rates*, *Recording transactions in foreign currencies* and *Setting a monthly allowance*,
  co-wrote *Finding transactions*, and contributed to *Applying the confirmed opening balance* (implementation,
  storage format and design alternatives) ([#47](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/47),
  [#55](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/55), [#83](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/83),
  [#103](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/103)).
* Wrote most of *Release scope*, *User stories*, *Planned enhancements* and the storage and recovery manual tests,
  and filled in the remaining placeholders ([#112](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/112)).
* UML: added the foreign-currency withdrawal sequence diagram ([#57](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/57)),
  and fixed the notation of the `Money` and `recent` sequence diagrams after peer review
  ([#100](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/100)).

### Contributions to team-based tasks

* Set up the issue labels used by the team (`type.*`, `priority.*`, `severity.*`).
* Opened labelled, milestoned issues for team work, e.g. [#61](https://github.com/AY2627S1-CS2113-W13-2/tp/issues/61)
  (categories, for a teammate) and the v2.1 issues [#95](https://github.com/AY2627S1-CS2113-W13-2/tp/issues/95)–[#99](https://github.com/AY2627S1-CS2113-W13-2/tp/issues/99).
* Merged 11 teammates' PRs after review, e.g. [#50](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/50),
  [#73](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/73), [#92](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/92).
* Fixed teammates' `@@author` tags so RepoSense credits their code correctly
  ([#105](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/105)).

### Review/mentoring contributions

* Reviewed 17 PRs: [#20](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/20), [#21](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/21),
  [#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43), [#50](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/50),
  [#54](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/54), [#63](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/63),
  [#64](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/64), [#73](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/73),
  [#77](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/77), [#80](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/80),
  [#85](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/85), [#86](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/86),
  [#92](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/92), [#94](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/94),
  [#109](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/109), [#110](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/110),
  [#121](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/121)
* Examples: in [#50](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/50), caught two compile errors and a bug
  where `Money` printed `-0.004` as `-$0.00`. In [#43](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/43),
  pointed out that a hand-edited save file could load amounts and rates above the limits; they are now checked on load.
  In [#92](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/92), left 11 comments, including `@@author` tags that
  claimed other members' code, and notation errors in two new UML diagrams.

### Contributions beyond the project team

* Reviewed the Developer Guide of TripJio (CS2113-W10-1):
  [review](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/1#pullrequestreview-5700688464)
* <!-- TODO: PE-D bug reports, forum posts -->
