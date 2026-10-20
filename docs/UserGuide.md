# User Guide

## Introduction

FinNUS is a desktop finance tracker designed for tech-savvy university students managing a tight monthly budget. Whether you are an international student juggling foreign currency allowances or a local student managing daily campus expenses, FinNUS provides a streamlined Command Line Interface (CLI) to help you determine exactly how much is safe to spend each day.

## Quick Start

1. Ensure that Java 25 or later is installed on your computer.
   - Mac users: Ensure you have the precise JDK version prescribed here.
2. Download the latest `finnus.jar` file from here.
3. Copy the file to the folder you want to use as the home folder for your FinNUS.
4. Open a terminal, `cd` to the folder containing the JAR file, and run the application using the following command:

   ```bash
   java -jar finnus.jar
   ```

5. Type a command in the CLI and press Enter to execute it. For example, type `help` and press Enter to display a table of command names, descriptions and syntax so you can look up how to use them.
6. Refer to the Features section below for details of each command.

## Saving and recovering your data

FinNUS automatically saves your transactions, opening balance, and exchange rates after a successful command changes
financial state. Reopening the app restores these values, including transaction order and exact decimal amounts.
There is no separate save command. Read-only commands normally do not write files.

### Save location

The save file, `finnus.txt`, lives directly beside `finnus.jar`. This location is based on the JAR's folder, so launching
the same JAR from another terminal folder still opens the same saved data. Moving the JAR to another folder requires
moving its save file too. When running through Gradle or an IDE, the file lives in the working directory, normally the
project folder. No save file is created until financial state changes.

FinNUS writes a complete snapshot through a temporary file before replacing the save file. After subsequent saves,
`finnus.txt.bak` contains the previous successfully saved state. Temporary writes and replacement failures do not
deliberately truncate the existing save. A filesystem that does not support atomic replacement cannot save;
FinNUS reports the failure and retains your changes in memory.

Use one running FinNUS instance per save file. Close the app before editing, moving, or replacing its data files.

### Save failures

If an automatic save fails, FinNUS displays a warning. Your changes remain available in the current session, but they
are not yet safely stored. The app retries after a later successful command, including a read-only command, and before
exiting or reaching the end of input. If saving still fails at shutdown, a final warning explains that unsaved changes
will be lost. The previous saved state remains available for the next session.

If the file could not be read at startup, or its format version is unsupported, saving is disabled for that session.
Commands can still run in memory, but their changes will not survive exit. Correct the file access problem or use a
compatible FinNUS version, then restart the app; it does not reload external edits during an active session.

### Corrupt data and recovery

* **Some rows are corrupt:** FinNUS loads valid rows and reports skipped row numbers and reasons. Missing or invalid
  opening balances default to zero with a warning. Balances may be incomplete because some financial history was skipped.
* **The main file is missing or wholly corrupt:** FinNUS attempts to load a fully valid `finnus.txt.bak` and reports
  backup recovery. A partially corrupt backup is not used automatically.
* **No saved state can be recovered:** FinNUS warns that the save is corrupted and starts with an empty ledger in memory.
  The original file is left untouched at startup. If you want to inspect or repair it, exit without adding or deleting
  transactions or changing the opening balance or exchange rates. Open the file path shown in the warning, repair it,
  then reopen FinNUS. Read-only commands and exit do not replace the original.

Before a later save replaces damaged data, FinNUS preserves the original bytes in a uniquely named
`finnus.txt.corrupt-*` file in the same folder. A damaged backup is likewise preserved as `finnus.txt.bak.corrupt-*`
before it is replaced. If preservation fails, FinNUS refuses that save and keeps the changes in memory.
Recovered sessions do not automatically rewrite files during startup or exit unless financial state has changed.

For manual backup restoration, close FinNUS, keep a separate copy of the damaged file, copy a known valid backup to
`finnus.txt`, and restart. Restoring a backup returns to that earlier saved state; newer changes may be absent.
Recovery detects malformed records and invalid values, but cannot identify valid-looking manual edits as mistakes.

### File format for manual repair

Files use UTF-8, a version header, and one pipe-separated record per line:

```text
FINNUS|1
OPENING|100.00
RATE|JPY|110
TRANSACTION|DEPOSIT|50.00|QWxsb3dhbmNl
TRANSACTION|WITHDRAWAL|4.50|THVuY2g=
```

* Keep the `FINNUS|1` header and one non-negative opening balance.
* Transaction types are `DEPOSIT` and `WITHDRAWAL`. Amounts and foreign exchange rates must be positive plain decimals.
  Scientific notation is not accepted. Transaction records are stored oldest first; identical rows are kept as
  separate transactions.
* Rate codes must be valid uppercase ISO currency codes other than SGD. The first valid opening balance and the first
  valid rate for each currency are used; later duplicates are skipped with warnings.
* Descriptions are UTF-8 text encoded using Base64. The example descriptions are `Allowance` and `Lunch`.
  An empty description leaves the final field empty, for example `TRANSACTION|DEPOSIT|5|`.
  Encode descriptions before editing this field so pipes, Unicode, and line breaks remain safe. Base64 is not encryption.
* Blank lines, Windows or Unix line endings, and an optional UTF-8 byte-order mark are accepted.

## Features 

### Adding a deposit: `deposit`
Records money you receive, such as your monthly allowance, and adds it to your balance.

Format: `deposit AMOUNT [d/DESCRIPTION]`

* `AMOUNT` is in SGD and must be a positive number with at most 2 decimal places, e.g. `500`, `12.5` or `4.50`.
* `AMOUNT` must not be more than `1000000`. Do not include `$` or commas.
* `DESCRIPTION` is optional. Everything after `d/` is taken as the description.
* `AMOUNT` and `d/DESCRIPTION` can be given in either order.

Examples:

* `deposit 500 d/Monthly Allowance`
* `deposit d/Part-time job 120.50`
* `deposit 50`

Expected output for `deposit 500 d/Monthly Allowance`:
```
____________________________________________________________
Deposit added:
  [Deposit] +$500.00 (Monthly Allowance)
You now have 1 transaction(s).
____________________________________________________________
```

### Adding a withdrawal: `withdraw`
Records money you spend and removes it from your balance.

Format: `withdraw AMOUNT d/DESCRIPTION`

* `AMOUNT` follows the same rules as for `deposit`.
* `DESCRIPTION` is required, so you always know what the money was spent on.
* `AMOUNT` and `d/DESCRIPTION` can be given in either order.

Examples:

* `withdraw 4.50 d/Lunch`
* `withdraw d/Chicken rice 4.50`

Expected output for `withdraw 4.50 d/Lunch`:
```
____________________________________________________________
Withdrawal added:
  [Withdrawal] -$4.50 (Lunch)
You now have 2 transaction(s).
____________________________________________________________
```

Example of an invalid command, `withdraw 4.50` (no description):
```
____________________________________________________________
Error: A withdrawal needs a description so you know what you spent on.
Format: withdraw AMOUNT d/DESCRIPTION
____________________________________________________________
```

### Setting your opening balance: `opening`
Sets how much money you had before you started using FinNUS, so your balance starts from an accurate figure.

Format: `opening AMOUNT`

* `AMOUNT` follows the same rules as for `deposit`, except that `0` is also allowed.
* If you never set it, your opening balance is `0`.
* Running `opening` again replaces the previous value. It does not add a transaction, so it will not show up in `recent` and cannot be removed with `delete`.
* Changing the opening balance also changes your balance by the same difference, even if you have already recorded transactions.

Examples:

* `opening 1200.50`
* `opening 0` (resets the opening balance)

Expected output for `opening 1200.50` after recording a deposit of `500` and a withdrawal of `4.50`:
```
____________________________________________________________
Opening balance updated: $0.00 -> $1200.50
Your balance is now $1696.00.
Note: this also changes the balance for your 2 existing transaction(s).
____________________________________________________________
```

### Setting an exchange rate: `rate`
Sets how much of another currency 1 SGD buys, so FinNUS can convert amounts in that currency to SGD.
Rates are entered by you rather than fetched online, so FinNUS works without internet.

Format: `rate CURRENCY RATE` to set a rate, or `rate` to list all rates.

* `CURRENCY` is a 3-letter currency code such as `JPY`, `USD` or `MYR`, in upper or lower case. `SGD` cannot be given a rate, because all amounts are already in SGD.
* `RATE` is how many units of `CURRENCY` 1 SGD buys, e.g. `110` for 1 SGD = 110 JPY. It must be a positive number with at most 6 decimal places, and not more than `1000000`.
* Setting a rate for a currency that already has one replaces the old rate.

Examples:

* `rate JPY 110`
* `rate usd 0.74`
* `rate`

Expected output for `rate JPY 110`:
```
____________________________________________________________
Exchange rate set: 1 SGD = 110 JPY
____________________________________________________________
```

Expected output for `rate` after setting rates for JPY and MYR:
```
____________________________________________________________
Exchange rates:
  1 SGD = 110 JPY
  1 SGD = 3.45 MYR
____________________________________________________________
```

### Viewing balance: `balance`
Shows the remaining balance of the user's account. 

Format: `balance`

* No parameters required. Additional text after `balance` is ignored.
* Does not modify existing transactions

Example: `balance` or `balance 123`

Expected output:
```text
____________________________________________________________
Your curent balance is: $100.00

____________________________________________________________
```

### Viewing recent transactions: `recent`
Shows the user's most recent tranactions, ordered by recency where the newest transaction is shown first.

Format: `recent [NUMBER]`

* `[NUMBER]` is optional. It is set to 5 by default.
* If `[NUMBER]` is more than the total number of transactions, the program outputs the entire list of transactions.
* `[NUMBER]` must be positive 

Examples:

* `recent`
* `recent 10`

Example output for `recent 2`:
```text
Here is a list of the recent 2 transaction(s):
1. [Deposit] +$30.50
2. [Withdraw] -$30.00 (Food)

```

### Deleting a transaction: `delete`
Deletes a transaction permanently.

Format: `delete INDEX`

* `INDEX` uses the list of recent transactions as reference. Use `recent` to get the necessary index of the transaction to delete.
* `INDEX` must be within the range of the transaction list's size.

Example:

* `delete 1` permanently deletes the most recent transaction.

Example output for `delete 3`:
```text
Transaction deleted: [Withdrawal] -$30.00 (BBQ)

```

### Viewing command usage: `help`
Displays a table of command names, descriptions and syntax so you can look up how to use them.

Format: `help`

* No parameters are required. Extra text after `help` is ignored, so `help 123` displays the same table as `help`.
* Help does not change your transactions or exit the application.
* In the displayed syntax, `[]` means optional.

Example: `help` or `help 123`

Expected output:
```text
____________________________________________________________
Available commands:
+----------+-----------------+--------------------------------+
| Command  | Description     | Syntax                         |
+----------+-----------------+--------------------------------+
| deposit  | Record income   | deposit AMOUNT [d/DESCRIPTION] |
| withdraw | Record spending | withdraw AMOUNT d/DESCRIPTION  |
| recent   | Recent activity | recent [NUMBER]                |
| delete   | Delete a record | delete INDEX                   |
| opening  | Opening balance | opening AMOUNT                 |
| rate     | Manage FX rates | rate [CURRENCY RATE]           |
| help     | List commands   | help                           |
| exit     | Close FinNUS    | exit                           |
+----------+-----------------+--------------------------------+

[] means optional.
____________________________________________________________
```

## FAQ

**Q**: How do I transfer my data to another computer? 

**A**: {your answer here}

## Command Summary

{Give a 'cheat sheet' of commands here}

* Add a deposit: `deposit AMOUNT [d/DESCRIPTION]`
* Add a withdrawal: `withdraw AMOUNT d/DESCRIPTION`
* View recent transactions: `recent [NUMBER]`
* Delete a transaction: `delete INDEX`
* Set opening balance: `opening AMOUNT`
* Set or list exchange rates: `rate [CURRENCY RATE]`
* View command usage: `help`
* Exit the application: `exit`
