# User Guide

## Introduction

{Give a product intro}

## Quick Start

{Give steps to get started quickly}

1. Ensure that you have Java 25 or above installed.
1. Down the latest version of `Duke` from [here](http://link.to/duke).

## Features 

{Give detailed description of each feature}

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

## FAQ

**Q**: How do I transfer my data to another computer? 

**A**: {your answer here}

## Command Summary

{Give a 'cheat sheet' of commands here}

* Add a deposit: `deposit AMOUNT [d/DESCRIPTION]`
* Add a withdrawal: `withdraw AMOUNT d/DESCRIPTION`
* Set opening balance: `opening AMOUNT`
