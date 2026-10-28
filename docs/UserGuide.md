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

5. The first time you run FinNUS, it asks for your opening balance. See [First-run setup](#first-run-setup-opening-balance).
6. Type a command in the CLI and press Enter to execute it. For example, type `help` and press Enter to display a table of command names, descriptions and syntax so you can look up how to use them.
7. Refer to the Features section below for details of each command.

## Saving and recovering your data

FinNUS automatically saves transactions, opening balance, and exchange rates whenever a command changes them.
The save file is `finnus.txt`.

If a data row is corrupt, FinNUS skips it and reports a warning, the recovered ledger may be incomplete.
If the main file is unusable, FinNUS tries its backup. If neither can be recovered, it starts with an empty ledger in memory and leaves the original file untouched until data changes.

## Features 

### Adding a deposit: `deposit`
Records money you receive, such as your monthly allowance, and adds it to your balance.

Format: `deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY]`

* `AMOUNT` is in SGD (unless `c/CURRENCY` is given) and must be a positive number with at most 2 decimal places, e.g. `500`, `12.5` or `4.50`.
* `AMOUNT` must not be more than `1000000`. Do not include `$` or commas.
* `DESCRIPTION` is optional. Everything after `d/` is taken as the description.
* `AMOUNT` and `d/DESCRIPTION` can be given in either order.
* To record an amount in another currency, add `c/CURRENCY`. See [Recording an amount in another currency](#recording-an-amount-in-another-currency-c).

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

Format: `withdraw AMOUNT d/DESCRIPTION [c/CURRENCY]`

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

### First-run setup: opening balance
The first time you run FinNUS, it asks how much money you currently have, so your balance starts from an accurate figure.

* Enter the amount in SGD. It follows the same rules as for `deposit`, except that `0` is also allowed.
* Answer `yes` (or `y`) to confirm, or `no` (or `n`) to enter a different amount.
* The opening balance is not a transaction, so it does not show up in `recent` and cannot be removed with `delete`.
* Setup only happens once. Afterwards, `opening` is not a command.

Example of first-run setup, entering `1200.50` and then `yes`:
```
____________________________________________________________
First-time setup
Enter your current opening balance in SGD.
This is your starting balance and will not be recorded as a transaction.
____________________________________________________________
Opening balance (SGD): 1200.50
Confirm opening balance $1200.50 SGD? [yes/no]: yes
____________________________________________________________
Welcome to FinNUS! Type a command to get started.
____________________________________________________________
```

### Setting an exchange rate: `rate`
Sets how much of another currency 1 SGD buys, so FinNUS can convert amounts in that currency to SGD.
Rates are entered by you rather than fetched online, so FinNUS works without internet.

Format: `rate CURRENCY RATE` to set a rate, or `rate` to list all rates.

* `CURRENCY` is a 3-letter currency code such as `JPY`, `USD` or `MYR`, in upper or lower case. `SGD` cannot be given a rate, because all amounts are already in SGD.
* `RATE` is how many units of `CURRENCY` 1 SGD buys, e.g. `110` for 1 SGD = 110 JPY. It must be a positive number with at most 6 decimal places, and not more than `1000000`.
* Setting a rate for a currency that already has one replaces the old rate, and the old rate is shown.
* If no rates are set yet, `rate` shows `No exchange rates set yet.` with an example.

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

Expected output for `rate JPY 112` when JPY already has a rate:
```
____________________________________________________________
Exchange rate set: 1 SGD = 112 JPY (was 1 SGD = 110 JPY)
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

<!-- @@author chancheese -->
### Recording an amount in another currency: `c/`
Add `c/CURRENCY` to a `deposit` or `withdraw` to record an amount in another currency, e.g. while on exchange.
FinNUS converts it to SGD using the rate you set with `rate`, and keeps the original amount so you can see it later.

Format: `deposit AMOUNT [d/DESCRIPTION] c/CURRENCY` or `withdraw AMOUNT d/DESCRIPTION c/CURRENCY`

* `CURRENCY` is a 3-letter currency code, in upper or lower case. You must set a rate for it first with `rate`.
* `AMOUNT` is in `CURRENCY` and must be a positive number with at most 2 decimal places. After converting, it is rounded to the nearest cent, and must be at least `$0.01` and at most `$1000000`.
* `c/CURRENCY` can be given anywhere after the command word, but only once. `c/SGD` is the same as leaving it out.
* The rate is saved with the transaction, so changing a rate later does not change transactions you have already recorded.
* Because `c/` marks the currency, a word starting with `c/` cannot be used inside a description.

Examples:

* `withdraw 1100 d/Ramen c/JPY`
* `deposit c/usd 50`

Expected output for `withdraw 1100 d/Ramen c/JPY` after `rate JPY 110`:
```
____________________________________________________________
Withdrawal added:
  [Withdrawal] -$10.00 (Ramen) [JPY 1100]
Converted at 1 SGD = 110 JPY.
You now have 1 transaction(s).
____________________________________________________________
```

Example of an invalid command, `deposit 50 c/USD` when no USD rate is set:
```
____________________________________________________________
Error: No exchange rate set for USD. Set one first with: rate USD RATE
____________________________________________________________
```
<!-- @@author -->

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
+----------+-----------------+---------------------------------------------+
| Command  | Description     | Syntax                                      |
+----------+-----------------+---------------------------------------------+
| deposit  | Record income   | deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY] |
| withdraw | Record spending | withdraw AMOUNT d/DESCRIPTION [c/CURRENCY]  |
| recent   | Recent activity | recent [NUMBER]                             |
| delete   | Delete a record | delete INDEX                                |
| balance  | Current balance | balance                                     |
| rate     | Manage FX rates | rate [CURRENCY RATE]                        |
| help     | List commands   | help                                        |
| exit     | Close FinNUS    | exit                                        |
+----------+-----------------+---------------------------------------------+

[] means optional.
____________________________________________________________
```

## FAQ

**Q**: How do I transfer my data to another computer? 

**A**: {your answer here}

## Command Summary

{Give a 'cheat sheet' of commands here}

* Add a deposit: `deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY]`
* Add a withdrawal: `withdraw AMOUNT d/DESCRIPTION [c/CURRENCY]`
* View recent transactions: `recent [NUMBER]`
* Delete a transaction: `delete INDEX`
* Set or list exchange rates: `rate [CURRENCY RATE]`
* View command usage: `help`
* Exit the application: `exit`
