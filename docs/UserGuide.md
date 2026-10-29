# User Guide

## Introduction

FinNUS is a desktop finance tracker designed for tech-savvy university students managing a tight monthly budget. Whether you are an international student juggling foreign currency allowances or a local student managing daily campus expenses, FinNUS provides a streamlined Command Line Interface (CLI) to help you determine exactly how much is safe to spend each day.

## Quick Start

1. Ensure that JDK 25 is installed on your computer.
   - On macOS with SDKMAN, select the project version with `sdk use java 25.0.3.fx-zulu`.
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

FinNUS automatically saves transactions, the opening balance, and exchange rates whenever they change. The main save
file is `finnus.txt`, with the previous saved state kept in `finnus.txt.bak`.

On a first run with no existing ledger or backup, FinNUS creates a pending setup file and asks for an opening balance.
The balance is saved only after you confirm it. If you exit or input ends before confirmation, the pending setup state
remains and FinNUS asks you to complete setup the next time you run it.

If a data row is corrupt, FinNUS skips it and reports a warning; the recovered ledger may be incomplete.
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

Format: `withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY]`

* `AMOUNT` follows the same rules as for `deposit`.
* `DESCRIPTION` is required, so you always know what the money was spent on.
* `AMOUNT` and `d/DESCRIPTION` can be given in either order.
* `CATEGORY` is optional. If you leave it out, the spending is recorded under `Others`.
  See [Categorising your spending](#categorising-your-spending-t-and-category).

Examples:

* `withdraw 4.50 d/Lunch`
* `withdraw d/Chicken rice 4.50 t/food`

Expected output for `withdraw 4.50 d/Lunch`:
```
____________________________________________________________
Withdrawal added:
  [Withdrawal] -$4.50 (Lunch) #Others
You now have 2 transaction(s).
____________________________________________________________
```

Example of an invalid command, `withdraw 4.50` (no description):
```
____________________________________________________________
Error: A withdrawal needs a description so you know what you spent on.
Format: withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY]
____________________________________________________________
```

### First-run setup: opening balance
The first time you run FinNUS, it asks how much money you currently have, so your balance starts from an accurate figure.

* Enter the amount in SGD. It follows the same rules as for `deposit`, except that `0` is also allowed.
* Answer `yes` (or `y`) to confirm, or `no` (or `n`) to enter a different amount.
* The opening balance is not a transaction, so it does not show up in `recent` and cannot be removed with `delete`.
* The ledger is not changed until you confirm. An invalid confirmation answer repeats the confirmation prompt.
* If you close the application or input ends before confirmation, setup is incomplete and will resume the next time you
  run FinNUS.
* After confirmation, setup is complete and normal commands begin. Afterwards, `opening` is not a command.

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
  [Withdrawal] -$10.00 (Ramen) [JPY 1100] #Others
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

<!-- @@author nabijonzzz -->
### Categorising your spending: `t/` and `category`
Add `t/CATEGORY` to a `withdraw` to record what kind of spending it was, so you can later see where your money went.
Every withdrawal has exactly one category. Deposits do not have categories.

Format: `withdraw AMOUNT d/DESCRIPTION t/CATEGORY`

* These categories are always available: `Food`, `Transport`, `Housing`, `Bills`, `Shopping`, `Entertainment`,
  `Education`, `Health` and `Others`.
* If you leave out `t/CATEGORY`, the withdrawal is recorded under `Others`.
* `CATEGORY` is not case-sensitive. It is shown in the spelling it was created with, so `t/FOOD` is shown as `#Food`.
* `t/CATEGORY` can be given anywhere after the command word, but only once. It also works together with `c/CURRENCY`.
* A category that does not exist is rejected, so a typo such as `t/fod` never creates a new category by accident.
* Because `t/` marks the category, a word starting with `t/` cannot be used inside a description.

Examples:

* `withdraw 4.50 d/Chicken rice t/food`
* `withdraw t/transport d/Bus 1.20`
* `withdraw 1100 c/JPY d/Ramen t/food`

Expected output for `withdraw 4.50 d/Chicken rice t/food`:
```
____________________________________________________________
Withdrawal added:
  [Withdrawal] -$4.50 (Chicken rice) #Food
You now have 1 transaction(s).
____________________________________________________________
```

#### Adding your own category: `category add`
Creates a custom category that you can then use with `t/`.

Format: `category add NAME`

* `NAME` is one word made of letters, digits and `-`, at most 20 characters, e.g. `Gym` or `bubble-tea`.
* `NAME` must not already exist, ignoring case. For example, `category add FOOD` is rejected because `Food` exists.

Example: `category add Gym`, then `withdraw 30 d/Membership t/gym`
```
____________________________________________________________
Category added: #Gym
Use it with, e.g.: withdraw 4.50 d/Lunch t/Gym
____________________________________________________________
```

#### Listing all categories: `category list`
Shows the predefined categories and any custom categories you have added.

Format: `category list`
```
____________________________________________________________
Categories:
  Predefined: Food, Transport, Housing, Bills, Shopping, Entertainment, Education, Health, Others
  Custom: Gym
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
Your current balance is: $100.00
____________________________________________________________
```

### Viewing recent transactions: `recent`
Shows the user's most recent transactions, ordered by recency with the newest transaction first.

Format: `recent [NUMBER]`

* `[NUMBER]` is optional. It is set to 5 by default.
* If `[NUMBER]` is more than the total number of transactions, the program outputs the entire list of transactions.
* `[NUMBER]` must be positive 

Examples:

* `recent`
* `recent 10`

Example output for `recent 2`:
```text
____________________________________________________________
Here is a list of the recent 2 transaction(s):
1. [Deposit] +$30.50
2. [Withdrawal] -$30.00 (Lunch) #Food
____________________________________________________________
```

<!-- @@author nabijonzzz -->
### Finding transactions: `find`
Lists the transactions whose description contains a keyword, so you can find a past transaction without scrolling
through all of them.

Format: `find KEYWORD`

* The search is not case-sensitive, and matches part of a word, e.g. `lun` matches `Lunch`.
* `KEYWORD` can be more than one word. The whole phrase is searched, e.g. `find chicken rice`.
* Only descriptions are searched, not amounts or categories.
* Results are shown from the most recent. Each one has the same number as in `recent`, so you can remove it straight
  away with `delete`.

Examples:

* `find lunch`
* `find chicken rice`

Expected output for `find lunch`:
```
____________________________________________________________
Found 2 transaction(s) matching "lunch":
1. [Withdrawal] -$12.00 (Lunch with friends) #Food
3. [Withdrawal] -$4.50 (Lunch) #Food
Use the number with delete to remove a transaction.
____________________________________________________________
```
<!-- @@author -->

### Deleting a transaction: `delete`
Deletes a transaction permanently.

Format: `delete INDEX`

* `INDEX` uses the list of recent transactions as reference. Use `recent` to get the necessary index of the transaction to delete.
* `INDEX` must be within the range of the transaction list's size.

Example:

* `delete 1` permanently deletes the most recent transaction.

Example output for `delete 3`:
```text
____________________________________________________________
Transaction deleted: [Withdrawal] -$30.00 (BBQ) #Food
____________________________________________________________
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
+----------+---------------------+---------------------------------------------------------+
| Command  | Description         | Syntax                                                  |
+----------+---------------------+---------------------------------------------------------+
| deposit  | Record income       | deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY]             |
| withdraw | Record spending     | withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY] |
| recent   | Recent activity     | recent [NUMBER]                                         |
| find     | Search descriptions | find KEYWORD                                            |
| delete   | Delete a record     | delete INDEX                                            |
| balance  | Current balance     | balance                                                 |
| rate     | Manage FX rates     | rate [CURRENCY RATE]                                    |
| category | Manage categories   | category add NAME or list                               |
| help     | List commands       | help                                                    |
| exit     | Close FinNUS        | exit                                                    |
+----------+---------------------+---------------------------------------------------------+

[] means optional.
____________________________________________________________
```

## FAQ

**Q**: How do I transfer my data to another computer?

**A**: Copy `finnus.txt` (and `finnus.txt.bak`, if it exists) from the folder containing `finnus.jar` into the folder
where you put `finnus.jar` on the other computer. FinNUS loads it automatically the next time it starts.

**Q**: What is the `finnus.log` file that appears next to `finnus.jar`?

**A**: It is a log file that records what FinNUS did, to help developers investigate problems. It is not needed to use
FinNUS, and it is safe to delete. Your financial data is stored in `finnus.txt`, not in the log.

**Q**: Why does `opening` say it is not a command?

**A**: The opening balance can only be set during the first-run setup. After that, record changes to your money with
`deposit` and `withdraw` instead.

## Command Summary

* Add a deposit: `deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY]`
* Add a withdrawal: `withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY]`
* View balance: `balance`
* View recent transactions: `recent [NUMBER]`
* Find transactions by description: `find KEYWORD`
* Delete a transaction: `delete INDEX`
* Set or list exchange rates: `rate [CURRENCY RATE]`
* Add a custom category: `category add NAME`
* List all categories: `category list`
* View command usage: `help`
* Exit the application: `exit`
