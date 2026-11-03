# User Guide

## Introduction

FinNUS is a desktop finance tracker designed for tech-savvy university students managing a tight monthly budget. Whether you are an international student juggling foreign currency allowances or a local student managing daily campus expenses, FinNUS provides a streamlined Command Line Interface (CLI) to help you track your spending, review balance, and manage your monthly allowance.

## Quick Start

1. Ensure that JDK 25 is installed on your computer.
   - On macOS with SDKMAN, select the project version with `sdk use java 25.0.3.fx-zulu`.
2. Download the latest `finnus.jar` from the [release page](https://github.com/AY2627S1-CS2113-W13-2/tp/releases).
3. Copy the file to the folder you want to use as the home folder for your FinNUS.
4. Open a terminal, `cd` to the folder containing the JAR file, and run the application using the following command:

   ```bash
   java -jar finnus.jar
   ```

5. The first time you run FinNUS, it asks for your opening balance, then offers optional balance-alert and monthly-allowance settings. See [First-run setup](#first-run-setup-opening-balance).
6. Type a command in the CLI and press Enter to execute it. For example, type `help` and press Enter to display a table of command names, descriptions and syntax so you can look up how to use them.
7. Refer to the Features section below for details of each command.

<!-- @@author naieel04 -->

## Saving and recovering your data

<!-- @@author -->

FinNUS automatically saves transactions, the opening balance, exchange rates, custom categories, allowance settings,
and spending-alert settings whenever they change. The main save file is `finnus.txt`, with the previous saved state kept
in `finnus.txt.bak`.
Successful transaction edits are saved automatically too; an unchanged edit does not create a new save by itself.

<!-- @@author lololol8484 -->
On a first run with no existing ledger or backup, FinNUS creates a pending setup file and asks for an opening balance.
The balance is saved only after you confirm it. If you exit or input ends before confirmation, the pending setup state
remains and FinNUS asks you to complete setup the next time you run it.
<!-- @@author -->

If a data row is corrupt, FinNUS skips it and reports a warning; the recovered ledger may be incomplete.
If the main file is unusable, FinNUS tries its backup. If neither can be recovered, it starts with an empty ledger in memory and leaves the original file untouched until data changes.

## Features 

<!-- @@author nabijonzzz -->
Every transaction records the day it happened, shown at the start, e.g. `[30 Oct 2026]`. The examples below assume
that today is 30 Oct 2026.
<!-- @@author -->

### Adding a deposit: `deposit`
Records money you receive, such as your monthly allowance, and adds it to your balance.

Format: `deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY] [on/DATE]`

* `AMOUNT` is in SGD (unless `c/CURRENCY` is given) and must be a positive number with at most 2 decimal places, e.g. `500`, `12.5` or `4.50`.
* `AMOUNT` must not be more than `1000000`. Do not include `$` or commas.
* `DESCRIPTION` is optional. Everything after `d/` is taken as the description.
* `AMOUNT` and `d/DESCRIPTION` can be given in either order.
* `DATE` is optional and defaults to today. See [Recording the date of a transaction](#recording-the-date-of-a-transaction-on).
* To record an amount in another currency, add `c/CURRENCY`. See [Recording an amount in another currency](#recording-an-amount-in-another-currency-c).

Examples:

* `deposit 500 d/Monthly Allowance`
* `deposit d/Part-time job 120.50`
* `deposit 50`

Expected output for `deposit 500 d/Monthly Allowance`:
```
____________________________________________________________
Deposit added:
  [30 Oct 2026] [Deposit] +$500.00 (Monthly Allowance)
You now have 1 transaction(s).
____________________________________________________________
```

### Adding a withdrawal: `withdraw`
Records money you spend and removes it from your balance.

Format: `withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY] [on/DATE]`

* `AMOUNT` follows the same rules as for `deposit`.
* `DESCRIPTION` is required, so you always know what the money was spent on.
* `AMOUNT` and `d/DESCRIPTION` can be given in either order.
* `DATE` is optional and defaults to today. See [Recording the date of a transaction](#recording-the-date-of-a-transaction-on).
* `CATEGORY` is optional. If you leave it out, the spending is recorded under `Others`.
  See [Categorising your spending](#categorising-your-spending-t-and-category).
* If the withdrawal crosses a spending threshold, FinNUS first asks you to confirm it. For example, your first withdrawal
  usually does this, because it takes your balance below your opening balance. See [Spending alerts](#spending-alerts).
  The expected outputs in this guide show withdrawals that do not cross a threshold.

Examples:

* `withdraw 4.50 d/Lunch`
* `withdraw d/Chicken rice 4.50 t/food`

Expected output for `withdraw 4.50 d/Lunch`:
```
____________________________________________________________
Withdrawal added:
  [30 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Others
You now have 2 transaction(s).
____________________________________________________________
```

Example of an invalid command, `withdraw 4.50` (no description):
```
____________________________________________________________
Error: A withdrawal needs a description so you know what you spent on.
Format: withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY] [on/DATE]
____________________________________________________________
```

### First-run setup: opening balance
The first time you run FinNUS, it asks how much money you currently have, so your balance starts from an accurate figure.

* Enter the amount in SGD. It follows the same rules as for `deposit`, except that `0` is also allowed.
* Answer `yes` (or `y`) to confirm, or `no` (or `n`) to enter a different amount.

<!-- @@author naieel04 -->

* The opening balance is not a transaction, so it does not show up in `recent` and cannot be changed with `edit`
  or removed with `delete`.

<!-- @@author -->

* The ledger is not changed until you confirm. An invalid confirmation answer repeats the confirmation prompt.
* The confirmed balance is saved before optional questions begin. If you close the application during those
  questions after a successful save, the next run uses that balance. You can configure optional settings later
  with `allowance` and `alert`. If saving fails, FinNUS warns you and keeps the changes in memory for retry.
<!-- @@author lololol8484 -->
* If you close the application or input ends before confirmation, setup is incomplete and will resume the next time you
  run FinNUS.
* After confirmation, FinNUS asks for an optional [balance alert threshold](#spending-alerts), then asks about your
  [allowance](#setting-your-monthly-allowance-allowance). If you enter an allowance amount, it also asks for an optional
  percentage threshold. You can skip these settings; normal commands begin afterwards, and `opening` is not a command.
<!-- @@author -->

Example of first-run setup, entering `1200.50` and then `yes`:
```
____________________________________________________________
First-time setup
Enter your current opening balance in SGD.
This is your starting balance and will not be recorded as a transaction.
____________________________________________________________
Opening balance (SGD): 1200.50
Confirm opening balance $1200.50 SGD? [yes/no]: yes
```

<!-- @@author chancheese -->
Next, FinNUS offers an optional balance-alert threshold before asking when your monthly allowance arrives and how much it
is. The threshold is the balance below which FinNUS warns you before a withdrawal. All of these settings are optional:

```
____________________________________________________________
Optional: set a balance threshold for spending alerts.
FinNUS will ask before a withdrawal takes your balance below it.
Press Enter to skip.
____________________________________________________________
Balance alert threshold in SGD: 100
```

Then, FinNUS asks about your monthly allowance:

* Enter the day of the month it arrives (1 to 31), then its amount in SGD.
* Press Enter at the day to skip the allowance, or at the amount to only count the days until it arrives.
* For an allowance in another currency, skip this and use [`allowance`](#setting-your-monthly-allowance-allowance)
  after setting an exchange rate.

Example, entering `5` and then `500` on 30 Oct 2026:
```
____________________________________________________________
Optional: when does your monthly allowance arrive?
FinNUS can count the days until it arrives and add it for you each month.
Press Enter to skip; you can set it later with: allowance 5 a/500
____________________________________________________________
Allowance day (1-31): 5
Allowance amount in SGD (Enter to only count days): 500
____________________________________________________________
Allowance set.
Allowance: $500.00 on day 5 (added automatically)
This cycle: 5 Oct 2026 - 4 Nov 2026
Next allowance in 6 days (5 Nov 2026).
Received this cycle: $0.00 in 0 deposit(s).
Spent this cycle: $0.00 in 0 withdrawal(s).
From 5 Nov 2026, FinNUS adds it as a deposit for you, so you do not need to record it.
____________________________________________________________
____________________________________________________________
Welcome to FinNUS! Type a command to get started.
____________________________________________________________
```
<!-- @@author -->

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
  To correct a recorded transaction's rate, use [`edit INDEX r/RATE`](#editing-a-transaction-edit).
* Because `c/` marks the currency, a word starting with `c/` cannot be used inside a description.
* A prefix must be followed by its value. For example, `c/ on/2026-01-15` is rejected as a missing currency rather
  than being interpreted as a currency followed by a date.

Examples:

* `withdraw 1100 d/Ramen c/JPY`
* `deposit c/usd 50`

Expected output for `withdraw 1100 d/Ramen c/JPY` after `rate JPY 110`:
```
____________________________________________________________
Withdrawal added:
  [30 Oct 2026] [Withdrawal] -$10.00 (Ramen) [JPY 1100] #Others
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

Format: `withdraw AMOUNT d/DESCRIPTION [t/CATEGORY]`

* These categories are always available: `Food`, `Transport`, `Housing`, `Bills`, `Shopping`, `Entertainment`,
  `Education`, `Health` and `Others`.
* If you leave out `t/CATEGORY`, the withdrawal is recorded under `Others`.
* `CATEGORY` is not case-sensitive. It is shown in the spelling it was created with, so `t/FOOD` is shown as `#Food`.
* `t/CATEGORY` can be given anywhere after the command word, but only once. It also works together with `c/CURRENCY`.
* A category that does not exist is rejected, so a typo such as `t/fod` never creates a new category by accident.
* Because `t/` marks the category, a word starting with `t/` cannot be used inside a description.
<!-- @@author -->

<!-- @@author chancheese -->
To see your spending in one category and its total, use [`find t/CATEGORY`](#finding-transactions-find).
<!-- @@author -->

<!-- @@author nabijonzzz -->
Examples:

* `withdraw 4.50 d/Chicken rice t/food`
* `withdraw t/transport d/Bus 1.20`
* `withdraw 1100 c/JPY d/Ramen t/food`

Expected output for `withdraw 4.50 d/Chicken rice t/food`:
```
____________________________________________________________
Withdrawal added:
  [30 Oct 2026] [Withdrawal] -$4.50 (Chicken rice) #Food
You now have 1 transaction(s).
____________________________________________________________
```

### Managing categories: `category`
Use `category` to list all predefined and custom spending categories. Use `category NAME` to add a custom category that
you can then use with `t/`.

Formats: `category` or `category NAME`

* `NAME` is one word made of letters, digits and `-`, at most 20 characters, e.g. `Gym` or `bubble-tea`.
<!-- @@author nabijonzzz -->
* `NAME` must not already exist, ignoring case. For example, `category FOOD` is rejected because `Food` exists.
<!-- @@author lololol8484 -->
* `add` and `list` are valid category names when entered alone: `category add` creates `add`, and `category list` creates `list`.

Example: `category Gym`, then `withdraw 30 d/Membership t/gym`
<!-- @@author nabijonzzz -->
```
____________________________________________________________
Category added: #Gym
Use it with, e.g.: withdraw 4.50 d/Lunch t/Gym
____________________________________________________________
```

Example output for `category`:
```
____________________________________________________________
Categories:
  Predefined: Food, Transport, Housing, Bills, Shopping, Entertainment, Education, Health, Others
  Custom: Gym
____________________________________________________________
```
<!-- @@author -->

<!-- @@author nabijonzzz -->
### Recording the date of a transaction: `on/`
Every deposit and withdrawal records the day it happened. It is today by default, but you can add `on/DATE` to
record a transaction you forgot to enter earlier.

Format: `deposit AMOUNT [d/DESCRIPTION] on/DATE` or `withdraw AMOUNT d/DESCRIPTION on/DATE`

* `DATE` must be in `YYYY-MM-DD` format, e.g. `2026-10-20`, and must be a real date (`2026-02-30` is rejected).
* `DATE` must be on or after `2000-01-01`.
* `DATE` cannot be in the future, since you can only record money that has already moved.
* `on/DATE` can be given anywhere after the command word, but only once. It works together with `c/` and `t/`.
<!-- @@author lololol8484 -->
* Transactions are stored in calendar-date order; FinNUS does not record the time of day. `recent` and `find` show
  the newest dates first. For transactions on the same date, the most recently entered appears first. Undated
  transactions from older save files appear after dated transactions, with the most recently entered undated
  transaction first.
* Adding a backdated transaction can change the numbers shown by `recent` and `find`, so check the current list before
  using an index with `edit` or `delete`.
* If transaction rows in `finnus.txt` are out of date order, FinNUS sorts them in memory when it starts. The file is
  rewritten in that order the next time FinNUS saves a change; a read-only run leaves the file as it was.
<!-- @@author nabijonzzz -->
* Transactions saved by older versions of FinNUS have no date, and are shown without one.
* Because `on/` marks the date, a word starting with `on/` cannot be used inside a description.

Examples:

* `withdraw 4.50 d/Lunch on/2026-10-20`
* `deposit 500 d/Allowance on/2026-10-01`
* `withdraw 1100 c/JPY d/Ramen t/food on/2026-10-25`

Expected output for `withdraw 4.50 d/Lunch on/2026-10-20`:
```
____________________________________________________________
Withdrawal added:
  [20 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Others
You now have 1 transaction(s).
____________________________________________________________
```

Example of an invalid command, `withdraw 4.50 d/Lunch on/2026-11-01` (a future date):
```
____________________________________________________________
Error: The date 2026-11-01 is in the future. Transactions can only be recorded for today or earlier.
____________________________________________________________
```
<!-- @@author -->

<!-- @@author chancheese -->
### Setting your monthly allowance: `allowance`
Tells FinNUS when your allowance arrives and how much it is. FinNUS then adds it as a deposit each month, and shows
how many days your money has to last.

Format: `allowance DAY [a/AMOUNT] [c/CURRENCY]`, `allowance` or `allowance off`

* `DAY` is the day of the month the allowance arrives, from 1 to 31. In shorter months, the last day of the month is
  used, e.g. `31` becomes 28 Feb.
* `AMOUNT` follows the same rules as for `deposit`. Without `a/AMOUNT`, FinNUS only counts the days.
* `c/CURRENCY` gives the currency of the amount, as for [`deposit`](#recording-an-amount-in-another-currency-c). It
  needs `a/AMOUNT`.
* Your budget cycle runs from one allowance day to the day before the next, e.g. 5 Oct to 4 Nov.
* `allowance` on its own shows a summary of the current cycle. Running `allowance DAY ...` again replaces the old
  setting.
* `allowance off` stops it. Deposits that were already added are kept.

How the allowance is added:

* When FinNUS starts, it adds a deposit described as `Allowance` for each allowance day that has passed, dated on that
  day. If FinNUS was closed for two months, both months are added.
* It starts from the **next** allowance day, never an earlier one, so it will not double an allowance you already
  recorded yourself.
* An amount in another currency is converted at the exchange rate set at the time it is added. If no rate is set,
  FinNUS waits and adds it once you set one with `rate`.
* If you delete an allowance deposit, it is not added again. Changing the amount only affects later deposits.
* Editing an allowance deposit changes only that recorded entry. It does not change the allowance setting or cause
  the deposit to be added again. Use `allowance DAY a/AMOUNT` to change future allowances.

The summary counts deposits and withdrawals dated in the current cycle, in SGD. Transactions saved by older versions of
FinNUS have no date, so they are not counted.

Examples:

* `allowance 5 a/500`
* `allowance 25 a/60000 c/JPY`
* `allowance 31`
* `allowance`
* `allowance off`

Expected output for `allowance 5 a/500`:
```
____________________________________________________________
Allowance set.
Allowance: $500.00 on day 5 (added automatically)
This cycle: 5 Oct 2026 - 4 Nov 2026
Next allowance in 6 days (5 Nov 2026).
Received this cycle: $0.00 in 0 deposit(s).
Spent this cycle: $0.00 in 0 withdrawal(s).
From 5 Nov 2026, FinNUS adds it as a deposit for you, so you do not need to record it.
____________________________________________________________
```

Expected output for `allowance`, after recording an allowance and three withdrawals this cycle:
```
____________________________________________________________
Allowance: $500.00 on day 5 (added automatically)
This cycle: 5 Oct 2026 - 4 Nov 2026
Next allowance in 6 days (5 Nov 2026).
Received this cycle: $500.00 in 1 deposit(s).
Spent this cycle: $18.30 in 3 withdrawal(s).
____________________________________________________________
```

Expected output when FinNUS starts on or after 5 Nov 2026:
```
____________________________________________________________
Welcome to FinNUS! Type a command to get started.
____________________________________________________________
____________________________________________________________
Allowance added: [5 Nov 2026] [Deposit] +$500.00 (Allowance)
____________________________________________________________
```

Example of an allowance that cannot be added yet, `allowance 5 a/60000 c/JPY` with no rate set for JPY:
```
____________________________________________________________
Your allowance for 5 Nov 2026 was not added yet. No exchange rate set for JPY. Set one first with: rate JPY RATE
____________________________________________________________
```
<!-- @@author -->

<!-- @@author lololol8484 -->
### Spending alerts

FinNUS warns before a withdrawal crosses a spending threshold. It does not block large withdrawals or prevent your
balance from becoming negative. When a withdrawal crosses one or more thresholds, FinNUS lists all of them and asks
whether to proceed. Enter `yes` or `y` to record it; `no`, `n`, or end of input cancels it. An invalid answer repeats the
confirmation prompt.

During first-run setup, after confirming your opening balance, you can set either or both optional alerts:

* **Balance threshold:** enter an SGD amount such as `100`. FinNUS warns when a withdrawal takes your balance from at
  least that amount to below it. Press Enter to skip.
* **Allowance percentage:** if you enter an allowance amount during setup, you can enter a whole percentage from 1 to
  100, such as `90`. FinNUS warns when withdrawals dated in an allowance cycle move from at or below that percentage
  to above it. Press Enter to skip. If you skip the allowance amount, you can configure this threshold later with
  `alert allowance PERCENT`; it remains inactive until an allowance amount is set.

Three thresholds are always applied when their reference values exist: withdrawals exceeding 100% of the allowance
amount, balance falling below the opening balance, and balance falling below `$0.00`. The 100% allowance threshold is
inactive when no allowance amount is set. Allowance spending is calculated in SGD from dated withdrawals in the cycle;
older transactions without dates are excluded.

Each threshold alerts only when crossed. Further withdrawals do not repeat an alert while the value remains beyond that
threshold. If the value returns to the safe side and crosses again, FinNUS alerts again. A withdrawal can cross multiple
thresholds at once, in which case one confirmation lists all of them. For example, with an allowance of `$500.00` and a
90% alert, moving cycle spending from `$440.00` to `$451.00` prompts:

```
____________________________________________________________
Spending alert:
Withdrawals would exceed 90% of your allowance ($450.00).
Proceed with this withdrawal? [yes/no]: no
____________________________________________________________
Withdrawal cancelled.
____________________________________________________________
```

Alert settings are saved with your ledger and remain in effect after restarting FinNUS. If your opening balance is zero,
falling below the opening balance also means falling below zero, so both threshold descriptions may appear together.

<!-- @@author lololol8484 -->
You can view or change the two optional thresholds at any time:

* `alert` shows both optional settings and indicates when an allowance percentage will apply.
* `alert balance AMOUNT` sets or replaces the fixed balance threshold. `AMOUNT` must be greater than zero.
* `alert balance off` clears the fixed balance threshold.
* `alert allowance PERCENT` sets or replaces the allowance percentage, as a whole number from 1 to 100. It can be set
  before an allowance amount exists and takes effect once one is configured.
* `alert allowance off` clears the allowance percentage.

Changing one optional threshold leaves the other unchanged. Clearing either optional threshold does not disable the
automatic alerts.

Examples: `alert`, `alert balance 100`, `alert allowance 90`, `alert balance off`, `alert allowance off`.
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

<!-- @@author chancheese -->
If you have [set your allowance](#setting-your-monthly-allowance-allowance), `balance` also shows how many days are
left until it arrives:
```text
____________________________________________________________
Your current balance is: $581.70
Next allowance in 6 days (5 Nov 2026).
____________________________________________________________
```
<!-- @@author -->

### Viewing recent transactions: `recent`
Shows the user's most recent transactions, ordered by recency with the newest transaction first.

Format: `recent [NUMBER]`

* `[NUMBER]` is optional. It is set to 5 by default.
* If `[NUMBER]` is more than the total number of transactions, the program outputs the entire list of transactions.
* `[NUMBER]` must be positive 
* "Recent" uses transaction dates, not the order of commands. On the same date, the most recently entered entry
  appears first. Undated legacy entries follow all dated entries.

Examples:

* `recent`
* `recent 10`

Example output for `recent 2`:
```text
____________________________________________________________
Here is a list of the recent 2 transaction(s):
1. [30 Oct 2026] [Deposit] +$30.50
2. [29 Oct 2026] [Withdrawal] -$30.00 (Lunch) #Food

```

<!-- @@author nkyjStar -->
### Viewing transaction history: `history`
Shows every transaction in pages, with the newest transaction dates first, matching `recent`.
For transactions on the same date, the most recently entered appears first. Undated transactions from older save
files appear after dated transactions, with the most recently entered undated transaction first.
Adding a backdated transaction can change the numbers, so check the current list before using `delete`.
The numbers match `recent` and the indices used by `delete`.
The opening balance is not a transaction and is not shown.

Format: `history [n/NUMBER] [p/PAGE]`

* `n/NUMBER` sets the number of transactions per page; the default is 5. If the page size exceeds
  the remaining transaction count, the page shows all remaining entries.
* `p/PAGE` chooses the first page to show; the default is 1. Both fields are optional and can appear in either order.
* Numbers must be positive whole numbers no larger than 2,147,483,647. Zero or malformed input is
  rejected as invalid; a larger number or a page beyond the available transactions is out of range.
* While viewing history, only the following inputs are accepted:
    * `next` shows the next page; `prev` shows the previous page.
    * `p/PAGE` jumps to a page. Page 1 begins with the newest transaction.
    * `n/NUMBER` changes the page size and keeps the current page's first transaction at the top. A later `p/PAGE`
      jump uses page boundaries counted from transaction 1 at the new page size.
    * `stop` leaves history mode. Use it before other commands, including `edit`, `delete`, and `exit`.
* At either end of the history, `next` or `prev` reports that there is no further page and keeps the current page.
  An empty history shows an empty-list message; `stop` remains available.

Example: `history n/2 p/2` with five transactions:

```text
____________________________________________________________
Transaction history: 3-4 of 5 (2 per page):
3. [28 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Food
4. [27 Oct 2026] [Deposit] +$30.50
History controls: next, prev, p/PAGE, n/NUMBER, stop.
____________________________________________________________
```
<!-- @@author -->

<!-- @@author nkyjStar -->
### Summarising cash flow: `chart flow`

`chart flow` summarises deposit and withdrawal totals over a selected date range, grouped by calendar
period. Each period has two bars with exact SGD totals. Bars are scaled against the largest total on
the current page, so compare their lengths within that page. The opening balance is excluded.

Format: `chart flow from/START to/END int/PERIOD [p/PAGE]`

* Use dates in `YYYY-MM-DD` format. Both dates are included and must be from 2000-01-01 through today;
  `START` must not be after `END`.
* `PERIOD` is `DAY`, `WEEK`, `MONTH`, or `YEAR`, ignoring case. Weeks run Monday through Sunday.
* Each page shows up to four consecutive periods, earliest first. `p/PAGE` selects a page, starting at 1;
  page 1 is the default. Repeat the command with another page to view more periods. A page outside the
  requested range is rejected. Chart pages are separate command results, so `next`, `prev`, and `stop`
  are only used while viewing transaction history.
* Each period shows separate deposit and withdrawal totals, including zero totals. If the chosen dates cut
  through a week, month, or year, only transactions within the exact requested dates count, even though
  the label shows the complete calendar period. Older transactions without dates cannot be placed in a period.

Example: `chart flow from/2026-10-01 to/2026-10-31 int/WEEK p/2`

### Viewing spending by category: `chart spending`

`chart spending` shows one bar per category with its recorded SGD total beside it. Bar lengths are
scaled against the largest category total in that chart. Deposits and the opening balance are excluded.

Format: `chart spending [int/PERIOD on/DATE]`

* `chart spending` totals all recorded withdrawals by category, including older withdrawals without dates.
* Give both `int/PERIOD` and `on/DATE` to show the day, week, month, or year containing that date.
  `PERIOD` accepts `DAY`, `WEEK`, `MONTH`, or `YEAR`, ignoring case; weeks start on Monday.
  `DATE` uses `YYYY-MM-DD` and must be from 2000-01-01 through today.
  For example, `chart spending int/MONTH on/2026-10-21` shows October 2026 spending.
* Categories with no spending in the selected interval are omitted. Deposits are excluded; foreign-currency
  withdrawals use the SGD amount recorded at the time. A dated chart excludes transactions without dates.

Example: `chart spending int/MONTH on/2026-10-21`

<!-- @@author -->

<!-- @@author nabijonzzz -->
### Finding transactions: `find`
Lists the transactions whose description contains a keyword, so you can find a past transaction without scrolling
through all of them. You can also list your spending in one category.

Format: `find [KEYWORD] [t/CATEGORY]`

* The search is not case-sensitive, and matches part of a word, e.g. `lun` matches `Lunch`.
* `KEYWORD` can be more than one word. The whole phrase is searched, e.g. `find chicken rice`.
* Only descriptions are searched, not amounts.
* Results are shown from the most recent. Each one has the same number as in `recent`, so you can use it directly
  with `edit` or `delete`. These are global transaction numbers, not positions within the filtered results.

Examples:

* `find lunch`
* `find chicken rice`

Expected output for `find lunch`:
```
____________________________________________________________
Found 2 transaction(s) matching "lunch":
1. [30 Oct 2026] [Withdrawal] -$12.00 (Lunch with friends) #Food
3. [28 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Food
Received: $0.00 in 0 deposit(s).
Spent: $16.50 in 2 withdrawal(s).
Use the number with delete to remove a transaction.
____________________________________________________________
```
<!-- @@author -->

<!-- @@author chancheese -->
Below the results, `find` adds them up in SGD, in the same way as the
[`allowance`](#setting-your-monthly-allowance-allowance) summary: `Received` for deposits and `Spent` for withdrawals.
Amounts in other currencies count at the SGD value they were recorded with.

Searching by category:

* Give a `KEYWORD`, a `t/CATEGORY`, or both. With both, only transactions matching both are shown.
* `t/CATEGORY` works as in [`withdraw`](#categorising-your-spending-t-and-category): it is not case-sensitive, and a
  category that does not exist is rejected. It can be given anywhere after `find`, but only once.
* Deposits have no category, so a search with `t/CATEGORY` only shows withdrawals, and only the `Spent` total.

Examples:

* `find t/food`
* `find lunch t/food`

Expected output for `find t/food`:
```
____________________________________________________________
Found 3 transaction(s) in #Food:
1. [30 Oct 2026] [Withdrawal] -$12.00 (Lunch with friends) #Food
3. [28 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Food
4. [27 Oct 2026] [Withdrawal] -$10.00 (Ramen) [JPY 1100] #Food
Spent: $26.50 in 3 withdrawal(s).
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
Transaction deleted: [27 Oct 2026] [Withdrawal] -$30.00 (BBQ) #Food

```

<!-- @@author naieel04 -->

### Editing a transaction: `edit`
Corrects the amount, description, category, date or saved exchange rate of a deposit or withdrawal. Give at least one field, in any order, without duplicates. Omitted fields keep their values;
type and currency cannot change.

Format: `edit INDEX [a/AMOUNT] [d/DESCRIPTION] [t/CATEGORY] [on/DATE] [r/RATE]`

* `INDEX` must be a valid positive whole number from `recent`, `history` or `find`.
* `AMOUNT` is in the existing currency: positive, with at most 2 decimal places.
* `DESCRIPTION` can contain spaces. Empty `d/` clears a deposit's description but withdrawals require a nonblank description.
* `CATEGORY` must already exist and applies only to withdrawals. Use `t/Others` to reset it; names ignore case.
* `DATE` follows the [date rules](#recording-the-date-of-a-transaction-on). Changing it may change transaction numbers and require
  checking `recent` again before another edit or deletion.
* `RATE` applies only to foreign transactions and follows the [rate rules](#setting-an-exchange-rate-rate).
  Amount-only edits keep the saved rate; `r/` changes it without needing or changing a global rate.
* Prefixes `a/`, `d/`, `t/`, `on/`, `r/` and `c/` cannot start words inside a description; `c/` is not accepted by `edit`.
* Successful changes are saved automatically. Invalid edits leave the selected entry unchanged;
  identical values print `No changes made.`
* A withdrawal edit that crosses a spending-alert threshold asks for confirmation before saving. This includes
  changes to the amount, saved exchange rate, or date. Answering `no` or ending input leaves the entry unchanged.


Examples:

* `edit 1 a/6.50`
* `edit 2 d/Scholarship allowance`
* `edit 1 t/food on/2026-10-25`
* `edit 3 a/55000 r/100`

Expected output for `edit 1 a/6.50` when transaction 1 is a withdrawal of $4.50 for Lunch:
```text
____________________________________________________________
Transaction updated:
  Before: [30 Oct 2026] [Withdrawal] -$4.50 (Lunch) #Others
  After:  [30 Oct 2026] [Withdrawal] -$6.50 (Lunch) #Others
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

<!-- @@author -->

```text
____________________________________________________________
Available commands:
+----------------+----------------------+--------------------------------------+
| Command        | Description          | Syntax                               |
+----------------+----------------------+--------------------------------------+
| deposit        | Record income        | deposit AMOUNT [d/DESCRIPTION]       |
|                |                      | [c/CURRENCY] [on/DATE]               |
| withdraw       | Record spending      | withdraw AMOUNT d/DESCRIPTION        |
|                |                      | [c/CURRENCY] [t/CATEGORY] [on/DATE]  |
| recent         | Recent activity      | recent [NUMBER]                      |
| history        | Transaction history  | history [n/NUMBER] [p/PAGE]          |
| chart flow     | Cash flow by period  | chart flow from/START to/END         |
|                |                      | int/PERIOD [p/PAGE]                  |
| chart spending | Spending by category | chart spending [int/PERIOD on/DATE]  |
| find           | Search transactions  | find [KEYWORD] [t/CATEGORY]          |
| delete         | Delete a record      | delete INDEX                         |
| edit           | Edit a record        | edit INDEX [a/AMOUNT]                |
|                |                      | [d/DESCRIPTION] [t/CATEGORY]         |
|                |                      | [on/DATE] [r/RATE]                   |
| balance        | Current balance      | balance                              |
| allowance      | Monthly allowance    | allowance [DAY [a/AMOUNT]            |
|                |                      | [c/CURRENCY]] or off                 |
| alert          | Manage spending      | alert [balance AMOUNT or off] or     |
|                | alerts               | [allowance PERCENT or off]           |
| rate           | Manage FX rates      | rate [CURRENCY RATE]                 |
| category       | Manage categories    | category [NAME]                      |
| help           | List commands        | help                                 |
| exit           | Close FinNUS         | exit                                 |
+----------------+----------------------+--------------------------------------+

[] means optional.
____________________________________________________________
```

## FAQ

**Q**: How do I transfer my data to another computer?

**A**: Copy `finnus.txt` (and `finnus.txt.bak`, if it exists) from the folder containing `finnus.jar` into the folder
where you put `finnus.jar` on the other computer. FinNUS loads it automatically the next time it starts.

**Q**: What is the `finnus.log` file?

**A**: It is a log file that records what FinNUS did, to help developers investigate problems. It is not needed to use
FinNUS, and it is safe to delete. Your financial data is stored in `finnus.txt`, not in the log.
The log is created in the terminal's working directory, which can differ from the JAR's folder if you launch it
using an absolute path. Saved financial data remains beside the packaged JAR.

**Q**: Why does `opening` say it is not a command?

**A**: The opening balance can only be set during the first-run setup. After that, record changes to your money with
`deposit` and `withdraw` instead.

<!-- @@author chancheese -->
**Q**: My allowance was counted twice. What happened?

**A**: You probably recorded it with `deposit` as well as setting an amount with `allowance`. Remove one of them with
`delete`. FinNUS does not add a deleted allowance again.
<!-- @@author -->

## Command Summary

* Add a deposit: `deposit AMOUNT [d/DESCRIPTION] [c/CURRENCY] [on/DATE]`
* Add a withdrawal: `withdraw AMOUNT d/DESCRIPTION [c/CURRENCY] [t/CATEGORY] [on/DATE]`
* View balance: `balance`
* Set your monthly allowance: `allowance DAY [a/AMOUNT] [c/CURRENCY]`
* View or stop your allowance: `allowance` or `allowance off`

<!-- @@author naieel04 -->

* Manage spending alerts: `alert`, `alert balance AMOUNT`, `alert allowance PERCENT`,
  `alert balance off`, or `alert allowance off`

<!-- @@author naieel04 -->

* View recent transactions: `recent [NUMBER]`

<!-- @@author -->

* View transaction history: `history [n/NUMBER] [p/PAGE]`, then `next`, `prev`, `p/PAGE`, `n/NUMBER`, or `stop`
<!-- @@author nkyjStar -->
* Summarise cash flow: `chart flow from/START to/END int/PERIOD [p/PAGE]`
* View spending by category: `chart spending [int/PERIOD on/DATE]`
<!-- @@author -->
* Find transactions by description or category: `find [KEYWORD] [t/CATEGORY]`

<!-- @@author naieel04 -->

* Delete a transaction: `delete INDEX`
* Edit a transaction: `edit INDEX [a/AMOUNT] [d/DESCRIPTION] [t/CATEGORY] [on/DATE] [r/RATE]`

<!-- @@author -->

* Set or list exchange rates: `rate [CURRENCY RATE]`
* Add a custom category: `category NAME`
* List categories: `category`

<!-- @@author naieel04 -->

* View command usage: `help`
* Exit the application: `exit`

<!-- @@author -->
