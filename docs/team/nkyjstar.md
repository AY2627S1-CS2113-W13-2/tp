# Jayden Neo - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including
international students who receive allowances in foreign currencies. Users record deposits and withdrawals,
view their balance and transaction history, and explore cash flow and spending charts, all by typing short commands.
It is written in Java by a team of 5.

## Summary of Contributions

### Code Contributed

[Reposense Link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=nkyjstar)

### Enhancements Implemented

* **`recent [NUMBER]` command**([#16](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/16)):
    * Implemented the parser, command, and transaction-list retrieval needed to show the latest transactions in newest-first order. It shows up to five entries by default or the requested positive number, numbers the displayed entries for use with `delete`, and reports an empty ledger without crashing. 
    * The retrieval selects only the requested suffix of the stored transaction list and reverses that view.

* **`balance` command**([#20](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/20)):
    * Implemented command parsing, balance calculation in `TransactionList`, and formatted output through the UI. The calculation starts from the opening balance and adds each deposit or subtracts each withdrawal
    * It is recomputed when requested, so later transactions and deletions are reflected without maintaining a separate cached total.

* **Shared `Money` value type**([#50](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/50)):
    * Designed and introduced an immutable `Money` class around `BigDecimal`, then migrated transaction, parser, command, setup, storage, and test code to use it. 
    * With the new class, monetary arithmetic, sign handling, numeric equality, and display formatting now have one implementation rather than being repeated across features.
    * Calculations retain exact decimal values, while output rounds to cents only when displayed; storage retains the unrounded value.

* **Paginated `history` command** ([#92](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/92)):
    * Added a history viewing mode with configurable page size, page selection, and `next`, `prev`, and `stop`
      controls. Transaction numbers match `recent` and `delete`; changing the page size keeps the first visible
      transaction in place.
    * Validated malformed and oversized page numbers separately, handled empty and out-of-range pages, and
      tested command routing while the history view is active.

* **Financial charts** ([#115](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/115)):
    * Added `chart flow` to show deposit and withdrawal totals across dated calendar periods and `chart spending`
      to show withdrawal totals by category, either overall or for a selected period.
    * Shared day, week, month, and year boundaries through `ChartPeriod`, and rendered scaled text bars with
      exact SGD totals. Added parser and command tests for date ranges, pagination, and empty results.

* **Named validation regexes** ([#125](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/125)):
    * Replaced inline regexes in parsing and storage with named constants, sharing repeated allowance-day,
      unsigned-integer, and percentage rules. Updated stale help-table test spacing to match the aligned output.

### Contributions to the UG

* Wrote the User Guide sections for [`balance`](../UserGuide.md#viewing-balance-balance) and [`recent`](../UserGuide.md#viewing-recent-transactions-recent), covering command syntax, optional arguments, expected output, and how the commands behave when a requested count exceeds the number of transactions. [#27](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/27)
* Wrote the [`history`](../UserGuide.md#viewing-transaction-history-history) instructions for opening and
  navigating pages, changing page size, and handling invalid input
  ([#92](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/92)).
* Documented [`chart flow`](../UserGuide.md#summarising-cash-flow-chart-flow) and
  [`chart spending`](../UserGuide.md#viewing-spending-by-category-chart-spending), including date and period
  filters, pagination, and how totals and bars should be read
  ([#115](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/115)).

### Contributions to the DG

* Wrote `Representing money`, `viewing recent transactions` and `viewing the current balance`. ([#63](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/63))
* Created sequence diagrams for `Money`, and `balance`; exported them as images for the DG. Refined the diagrams to show command activation and static method calls accurately. ([#63](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/63))
* Added the `Money` class diagram and refined its relationships and notation
  ([#94](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/94)).
* Wrote the [paginated history](../DeveloperGuide.md#viewing-paginated-transaction-history) design and
  walkthrough, including navigation tradeoffs and class and sequence diagrams
  ([#92](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/92)).
* Wrote the [financial charts](../DeveloperGuide.md#financial-charts) design, alternatives, validation
  notes, and class and sequence diagrams
  ([#115](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/115)).

### Contributions to team-based tasks

* Created issues to track the implementation of relevant user stories meant for a milestone.
* Discussed and settled on the issue labelling convention - use priority, severity and type labels, do not use Github fields
* Repackaged the default folder template to make finnus the main package

### Review/mentoring contributions
* Reviewed PRs: [#11](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11),
[#17](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/17),
[#18](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/18),
[#23](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/23), 
[#49](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/49),
[#55](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/55),
[#57](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/57),
[#59](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/59),
[#100](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/100),
[#101](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/101).

### Contributions beyond the project team
* Reviewed the Developer Guide of CakeQueue (CS2113-W12-1)
[review](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/6#pullrequestreview-5695432599)
