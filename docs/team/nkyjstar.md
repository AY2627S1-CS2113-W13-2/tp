# Jayden Neo - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including
international students who receive allowances in foreign currencies. Users record deposits and withdrawals,
view their balance and recent transactions, and set exchange rates, all by typing short commands.
It is written in Java by a team of 5.

## Summary of Contributions

### Code Contributed

[Reposense Link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=nkyjstar)

### Enhancements Implemented

* **`recent [NUMBER]` command**([#16](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/16)):
    * Implemented the parser, command, and transaction-list retrieval needed to show the latest transactions in newest-first order. It shows up to five entries by default or the requested positive number, numbers the displayed entries for use with `delete`, and reports an empty ledger without crashing. 
    * The retrieval selects only the requested suffix of the stored list and reverses that view, preserving the transactions' insertion order.

* **`balance` command**([#20](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/20)):
    * Implemented command parsing, balance calculation in `TransactionList`, and formatted output through the UI. The calculation starts from the opening balance and adds each deposit or subtracts each withdrawal
    * It is recomputed when requested, so later transactions and deletions are reflected without maintaining a separate cached total.

* **Shared `Money` value type**([#50](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/50)):
    * Designed and introduced an immutable `Money` class around `BigDecimal`, then migrated transaction, parser, command, setup, storage, and test code to use it. 
    * With the new class, monetary arithmetic, sign handling, numeric equality, and display formatting now have one implementation rather than being repeated across features.
    * Calculations retain exact decimal values, while output rounds to cents only when displayed; storage retains the unrounded value.

### Contributions to the UG

* Wrote the User Guide sections for [`balance`](../UserGuide.md#viewing-balance-balance) and [`recent`](../UserGuide.md#viewing-recent-transactions-recent), covering command syntax, optional arguments, expected output, and how the commands behave when a requested count exceeds the number of transactions. [#27](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/27)

### Contributions to the DG

* Wrote `Representing money`, `viewing recent transactions` and `viewing the current balance`. ([#63](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/63))
* Created sequence diagrams for `Money`, and `balance`; exported them as images for the DG. Refined the diagrams to show command activation and static method calls accurately. ([#63](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/63))

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

* Examples: Verified the correctness of UML sequence diagrams for ForeignTransactionCommand in [#57](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/57#pullrequestreview-5670051976), suggested the adoption of conventional exception handling practice in [#11](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11#discussion_r4204387346), but we decided to use an app-wide handler instead via `FinNUSException`.

### Contributions beyond the project team
* Reviewed the Developer Guide of CakeQueue (CS2113-W12-1)
[review](https://github.com/NUS-CS2113-AY2627-S1/tp/pull/6#pullrequestreview-5695432599)