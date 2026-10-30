# Jayden Neo - Project Portfolio Page

## Overview

FinNUS is a command-line finance tracker for university students on a tight monthly budget, including
international students who receive allowances in foreign currencies. Users record deposits and withdrawals,
view their balance and recent transactions, and set exchange rates, all by typing short commands.
It is written in Java by a team of 5.

### Summary of Contributions

## Code Contributed

[Reposense Link](https://nus-cs2113-ay2627-s1.github.io/tp-dashboard/?search=nkyjstar)

## Enhancements Implemented

* **`recent [NUMBER]` command:** Implemented the parser, command, and transaction-list retrieval needed to show the latest transactions in newest-first order. It shows up to five entries by default or the requested positive number, numbers the displayed entries for use with `delete`, and reports an empty ledger without crashing. The retrieval selects only the requested suffix of the stored list and reverses that view, preserving the ledger's insertion order. I fixed an empty-list crash and added parser and transaction-list tests covering the result order, requested length, and empty ledger. [Implementation details](../DeveloperGuide.md#viewing-recent-transactions).

* **`balance` command:** Implemented command parsing, balance calculation in `TransactionList`, and formatted output through the UI. The calculation starts from the opening balance and adds each deposit or subtracts each withdrawal; it is recomputed when requested, so later transactions and deletions are reflected without maintaining a separate cached total. I added tests for command parsing and balance calculation, and made the displayed result consistently use two decimal places. The command also handles an empty transaction list by showing the opening balance. [Implementation details](../DeveloperGuide.md#viewing-the-current-balance).

* **Shared `Money` value type:** Designed and introduced an immutable `Money` class around `BigDecimal`, then migrated transaction, parser, command, setup, storage, and test code to use it. This was a cross-cutting change: monetary arithmetic, sign handling, numeric equality, and display formatting now have one implementation rather than being repeated across features. Calculations retain exact decimal values, while output rounds to cents only when displayed; storage retains the unrounded value. I reconciled the migration with first-run setup and persistence, and added dedicated tests for repeated decimal addition, negation, formatting, sub-cent values, and equality across values such as `10` and `10.00`. [Implementation details](../DeveloperGuide.md#representing-money).

## Contributions to the UG

* Wrote the User Guide sections for [`balance`](../UserGuide.md#viewing-balance-balance) and [`recent`](../UserGuide.md#viewing-recent-transactions-recent), covering command syntax, optional arguments, expected output, and how the commands behave when a requested count exceeds the number of transactions. Corrected the balance description so it accurately explains what the command displays.
* Wrote the [`delete`](../UserGuide.md#deleting-a-transaction-delete) section, explaining how to use the transaction numbers shown by `recent` as deletion indices, the valid index range, and an example of the output.

## Contributions to the DG

* Wrote [Representing money](../DeveloperGuide.md#representing-money), explaining how the immutable `Money` value type supports exact arithmetic across parsing, transactions, balance calculation, and storage. Documented its formatting and equality rules, the boundary between input validation and domain values, and the design tradeoff between `Money`, raw `BigDecimal`, and `double`.
* Wrote the implementation sections for [viewing recent transactions](../DeveloperGuide.md#viewing-recent-transactions) and [viewing the current balance](../DeveloperGuide.md#viewing-the-current-balance). Traced each command from parsing through the ledger and UI, described edge cases such as an empty transaction list, and explained why recent entries are selected without reordering the ledger and why the balance is recalculated on request.
* Created sequence diagrams for [`Money`](../diagrams/MoneySequenceDiagram.puml), [`recent`](../diagrams/RecentSequenceDiagram.puml), and [`balance`](../diagrams/BalanceSequenceDiagram.puml); exported them as images for the DG. Refined the diagrams to show command activation and static method calls accurately.

## Contributions to team-based tasks

* Created issues to track the implementation of relevant user stories meant for a milestone.
* Discussed and settled on the issue labelling convention - use priority, severity and type labels, do not use Github fields
* Created the Money class to extract and standardize money-related methods like formatting.
* Repackaged the default folder template to make finnus the main package

## Review/mentoring contributions
* Verified the correctness of UML sequence diagrams for ForeignTransactionCommand [here](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/57#pullrequestreview-5670051976)
* Validated the developer guide [here](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/55#pullrequestreview-5668726699)
* Suggested the adoption of conventional exception handling practice [here](https://github.com/AY2627S1-CS2113-W13-2/tp/pull/11#discussion_r4204387346), but we decided to use an app-wide handler instead via `FinNUSException`.