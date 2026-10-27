# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

### Representing money

`Money` is the shared value type for monetary amounts. The parser creates it from user input,
`Transaction` stores it, `TransactionList` uses it to calculate balances, and storage writes its
exact decimal value. Keeping these operations in one class prevents different commands from
making different choices about arithmetic and display. `Money` is a value object: two instances
represent the same amount when their numerical values are equal, even if they were constructed
from differently scaled decimals such as `10` and `10.00`.

#### Internal representation and operations

`Money` is `final`, holds a private final `BigDecimal`, and has a private constructor. Callers
create values through `Money.of(BigDecimal)`, `Money.of(String)`, or `Money.of(long)`. The factory
returns the shared `Money.ZERO` instance for any numerical zero; it creates a new `Money` for a
nonzero result. This means two references to zero need not identify two different objects.

Arithmetic does not change either operand. `add()` adds the wrapped `BigDecimal` values and
returns the result through `Money.of()`. `negate()` similarly returns the value with its sign
reversed. For example, if `first` represents `10.005` and `second` represents `0.10`,
`first.add(second)` gives a separate `Money` representing `10.105`, while `first` still represents
`10.005`. This matters when `TransactionList#getBalance()` repeatedly adds signed transaction
amounts: each step produces the next total instead of changing the opening balance or a
transaction's stored amount. A deposit's `Transaction#getSignedAmount()` returns its existing
`Money` object; a withdrawal calls `negate()` to obtain a negative value.

The following sequence shows an example usage of `Money`. It uses object names rather than one generic `Money` lifeline. The two
inputs and the nonzero sum are distinct objects:

![Sequence diagram for invoking Money methods](images\MoneySequenceDiagram.png)

`Money` does not require every value to be positive or below `Money.MAX_AMOUNT`.
`Parser#parseAmount()` enforces those rules for user-entered transaction amounts, while
`parseNonNegativeAmount()` permits zero for an opening balance. Keeping sign validation at the
input boundary allows `Money` to represent a withdrawal's negative effect and a negative
running balance. It also allows exact sub-cent values within the domain even though ordinary
user input is limited to two decimal places.

#### Formatting and equality

Calculations retain every decimal place. `format()` rounds only the displayed value to two
places with `RoundingMode.HALF_UP`, giving `$10.11` for the sum in the diagram. It puts a minus
sign before the dollar sign for a negative displayed amount. If a small negative value rounds
to zero, it displays `$0.00` rather than `-$0.00`. `formatWithSign()` is used for transaction
lines, where deposits and withdrawals need explicit `+` and `-` signs. For a nonzero sub-cent
amount that would otherwise appear as zero, it preserves the exact digits, such as
`-$0.004`, so the transaction's direction remains visible.

`toPlainString()` and `toBigDecimal()` expose the unrounded value for storage and integration;
`toString()` also returns the plain decimal representation. Formatting is therefore a
presentation step, not a change to the stored amount. `equals()` uses `BigDecimal.compareTo()`
so `Money.of("10")` equals `Money.of("10.00")`. `hashCode()` removes trailing zeros first,
keeping equal values consistent in hash-based collections. `compareTo()` uses numerical order,
which lets the parser compare an amount with `Money.MAX_AMOUNT`.

#### Design considerations

`double` would make arithmetic shorter, but values such as `0.10` cannot generally be stored
exactly in binary floating point. A raw `BigDecimal` in every command would preserve precision,
but each command would have to repeat sign operations, rounding, and output rules. `Money`
centralizes those rules while still using `BigDecimal` for exact arithmetic. The tradeoff is an
extra class and method calls instead of operators; in return, the balance and transaction
display use the same monetary behavior. `MoneyTest` checks exact repeated addition,
immutability after negation, rounding only for display, signed sub-cent output, and equality
across different decimal scales.

### Adding a deposit or withdrawal

The `deposit` and `withdraw` commands record money coming in and going out. Each one creates a
`Transaction` and adds it to the `TransactionList`, which is the single place where all
transactions are stored and where the balance is calculated.

#### Classes involved

* `Parser` turns the text typed by the user into a command object, and rejects invalid input by
  throwing a `FinNusException`.
* `DepositCommand` and `WithdrawCommand` are subclasses of the abstract `Command` class. Each
  overrides `execute(TransactionList, Ui)`.
* `Transaction` is an immutable record of one money movement. It stores a `TransactionType`
  (`DEPOSIT` or `WITHDRAWAL`), a positive `Money` amount and a description.
* `TransactionList` holds all transactions in the order they were added.

#### How a withdrawal is processed

Given below is what happens when the user enters `withdraw 4.50 d/Lunch`.

1. `FinNUS#run()` reads the line through `Ui` and passes it to `Parser#parse()`.
2. `Parser#parse()` reads the command word `withdraw` and calls `parseWithdraw("4.50 d/Lunch")`.
3. `splitAmountAndDescription()` splits the arguments into the amount `"4.50"` and the
   description `"Lunch"`. The `d/` prefix only counts when it starts a word, so text such as
   `and/or` inside a description is not mistaken for a prefix. The amount and `d/DESCRIPTION`
   may be given in either order, e.g. `withdraw d/Lunch 4.50`.
4. `parseAmount()` checks the amount and converts it to a `Money` value. For a withdrawal,
   `parseWithdraw()` also checks that the description is not empty.
5. The parser returns a new `WithdrawCommand(4.50, "Lunch")`.
6. `FinNUS` calls `execute()`, which creates a `Transaction` of type `WITHDRAWAL`, adds it to the
   `TransactionList` and shows a confirmation through `Ui`.

A deposit follows the same steps, except that the description is optional.

#### Validation rules

An amount is accepted only if it:

* matches `\d+(\.\d{1,2})?`, i.e. a plain number with at most 2 decimal places. This one
  check rejects inputs such as `-5`, `1e3`, `NaN`, `$5` and `4.555`;
* is greater than 0;
* is not more than `Money.MAX_AMOUNT` (1,000,000), which catches typos such as extra zeros.

If any rule fails, the parser throws a `FinNusException`. The main loop catches it and shows the
message to the user, so invalid input never stops the app.

#### Design considerations

**Aspect: how amounts are stored**

* **Alternative 1 (current choice):** `Money`, which wraps a `BigDecimal`.
  * Pros: stores decimal amounts exactly, so the balance never drifts, and keeps formatting in one place.
  * Cons: more verbose, e.g. `a.add(b)` instead of `a + b`, and `compareTo()` instead of `<`.
* **Alternative 2:** `double`.
  * Pros: simpler arithmetic.
  * Cons: most decimal values cannot be stored exactly (e.g. `0.1 + 0.2` gives
    `0.30000000000000004`), so balances would become slightly wrong over many transactions.

**Aspect: whether a withdrawal needs a description**

* **Alternative 1 (current choice):** a description is required for `withdraw` but optional for
  `deposit`.
  * Pros: users always know what they spent money on, which is the main purpose of tracking
    expenses. Most deposits are regular allowances, so a description adds little.
  * Cons: a withdrawal takes slightly longer to type.
* **Alternative 2:** the description is optional for both.
  * Pros: faster to type.
  * Cons: the transaction history becomes hard to understand later.

### Setting the opening balance

The opening balance is the money the user had before they started using FinNUS. It is set once,
during first-run setup, when no save file exists yet.

#### How it works

1. `FinNUS` sees that setup has not been completed and runs `Setup#run()`, which asks for the
   opening balance in SGD.
2. `Setup` checks the input with `Parser#parseNonNegativeAmount()`, which uses the same rules as
   deposits except that 0 is allowed. Invalid input shows an error and asks again.
3. `Setup` asks the user to confirm the amount with `yes` or `no`. Answering `no` asks for the
   amount again.
4. Once confirmed, `OpeningBalanceCommand#execute()` stores the amount with
   `TransactionList#setOpeningBalance()`, and the ledger is saved.

`opening` is marked as setup-only in `CommandWord`, so typing it in the normal command loop is
treated as an unknown command.

The opening balance is a separate field in `TransactionList`, not a `Transaction`.
`getBalance()` uses it as the starting value when adding up all transactions:

```java
return transactions.stream()
        .map(Transaction::getSignedAmount)
        .reduce(openingBalance, Money::add);
```

#### Design considerations

**Aspect: how the opening balance is stored**

* **Alternative 1 (current choice):** a separate field in `TransactionList`.
  * Pros: it never appears in `recent`, cannot be removed by `delete`, and is not counted as
    income.
  * Cons: storage has to save it separately from the transactions.
* **Alternative 2:** a special first `DEPOSIT` transaction.
  * Pros: no extra field.
  * Cons: it would look like real income in the history and could be deleted by accident.

### Setting exchange rates

The `rate` command stores exchange rates used to convert foreign-currency transactions
to SGD. `rate JPY 110` sets 1 SGD = 110 JPY, and `rate` on its own lists all rates.

#### How it works

1. `Parser#parseRate()` expects exactly two arguments, a currency code and a rate.
2. `parseCurrencyCode()` upper-cases the code and checks it with `java.util.Currency`, which
   rejects typos such as `JYP` without FinNUS keeping its own list. `SGD` is rejected because
   amounts are already in SGD.
3. `parseRateValue()` accepts a positive number with up to 6 decimal places (e.g. `0.5425`), up to
   `Parser.MAX_RATE`.
4. `RateCommand#execute()` gets the `CurrencyRates` from `TransactionList#getRates()` and calls
   `setRate()`. If the currency already had a rate, the old one is shown too.

`CurrencyRates` keeps the rates in a `TreeMap`, so `rate` lists them in alphabetical order.

#### Design considerations

**Aspect: where exchange rates come from**

* **Alternative 1 (current choice):** the user types each rate in.
  * Pros: works offline and gives the same result every time, which makes it easy to test.
  * Cons: rates have to be updated by hand.
* **Alternative 2:** fetch live rates from an online API.
  * Pros: always up to date.
  * Cons: needs internet access, and tests would give different results from day to day.

### Viewing recent transactions

The `recent [NUMBER]` command lets a user inspect the latest recorded activity without changing
the ledger. With no number, it shows up to five transactions; `recent 2` shows up to two. The
numbered result is also useful when choosing an index for `delete`, which uses the same
newest-first ordering. The opening balance is not a transaction, so it never appears here.

#### How it works

`FinNUS#run()` reads a line and passes it to `Parser#parse()`. For `recent`, the parser calls
`parseRecent()`. Blank arguments create a `RecentTransactionCommand` with a default length of five.
Otherwise, the parser converts the argument to an `int` and rejects a malformed or non-positive
length with `FinNusException`. The main loop shows that exception as an error and continues.

The command's `execute()` method requests the selected entries from
`TransactionList#getRecentTransactions(length)`. `TransactionList` stores entries in insertion
order, oldest first. It finds the first selected position using
`max(0, size - length)`, takes the sublist through the end, and returns its reversed, read-only
view. This handles a request longer than the ledger without a separate error case. If the ledger
is empty, it returns an empty list before attempting a sublist.

The command then numbers the returned entries starting at 1 and uses `Transaction#toString()`
for each line. The heading reports how many entries were actually returned, which can be less
than the requested length. `Ui#showMessage()` prints the heading and lines between dividers.
For an empty ledger, the command instead displays `Your list of transactions is currently empty.`

This sequence shows the normal path for `recent 2`:

![Sequence diagram of recent command](images\RecentCommandSequenceDiagram.png)

The `destroy` marker denotes the end of the command object's useful lifetime in this flow.
Java decides when its memory is actually reclaimed.

For example, if TransactionList contains a deposit followed by a withdrawal, `recent 2` prints the
withdrawal as `1.` and the deposit as `2.`. The displayed numbers are positions in the current
newest-first result, not permanent transaction IDs. Adding or deleting an entry can change them.

#### Design considerations

Keeping the TransactionList in insertion order makes recording a transaction a simple append. Reversing
only the selected sublist gives `recent` the order users need without rearranging stored data.
Another approach would store transactions newest first, but then every feature that assumes
insertion order would need to account for that choice. `getRecentTransactions()` returns a
read-only view so callers can display entries without directly editing the ledger. The command
only formats data; it does not add, remove or change transactions.

### Viewing the current balance

The `balance` command displays the current balance in SGD, including the opening balance and every
recorded deposit and withdrawal. It takes no required arguments; the current parser ignores
extra text after `balance`. Like `recent`, this command reads the ledger without changing it.

#### How it works

`Parser#parse()` creates a `BalanceCommand`. Its `execute()` method calls
`TransactionList#getBalance()` and passes the formatted result to `Ui#showMessage()`.
`TransactionList` is the single place that calculates the balance. It starts with
`openingBalance`, which defaults to `Money.ZERO`, then folds every transaction's signed amount
into that starting value:

```java
return transactions.stream()
        .map(Transaction::getSignedAmount)
        .reduce(openingBalance, Money::add);
```

`Transaction#getSignedAmount()` returns the positive amount for a deposit and its negation for
a withdrawal. For example, an opening balance of `$100.00`, a `$30.50` deposit and a `$4.50`
withdrawal give `$126.00`. With no transactions, the result is simply the opening balance.
Because the sum is recalculated whenever `balance` is invoked, deleting a transaction or changing the
opening balance is reflected in the next `balance` result.

`Money` wraps `BigDecimal`, so addition keeps decimal values exact. `Money#format()` rounds only
the displayed result to two decimal places using `HALF_UP` and adds the dollar sign. Thus the
calculation retains precision if a stored amount has more than two decimal places. Such amounts
are covered by the balance command tests, even though ordinary user input is limited to cents.
Negative balances are allowed and displayed with a minus sign when their rounded value is
nonzero.

The sequence below follows the calculation and display of one `balance` request:

![Sequence diagram of a balance command](images\BalanceSequenceDiagram.png)

The `destroy` marker denotes the end of the command object's useful lifetime in this flow.
Java decides when its memory is actually reclaimed.

#### Design considerations

Recalculating the balance on demand takes time proportional to the number of transactions, but
keeps one source of truth. A cached running balance would make reads faster, yet every deposit,
withdrawal, deletion and opening-balance update would have to maintain the cache correctly.
For this small ledger, the simpler calculation avoids that coordination. Both `recent` and
`balance` leave the ledger unchanged, so neither creates a new change to save. The application
may still retry a save that failed after an earlier command.
<!-- @@author chancheese -->
### Recording transactions in foreign currencies

Adding `c/CURRENCY` to `deposit` or `withdraw` (e.g. `withdraw 1100 d/Ramen c/JPY`) records an
amount typed in another currency. It is converted to SGD with the rate set by `rate`, and the
original amount is kept so it can be shown later, e.g. `[Withdrawal] -$10.00 (Ramen) [JPY 1100]`.

#### How it works

1. `Parser#extractCurrency()` removes the `c/CURRENCY` part from the arguments, so the rest is
   split into amount and description exactly as before. `c/SGD` counts as no currency.
2. If a currency was given, `parseForeignAmount()` checks the amount and the parser returns a
   `ForeignTransactionCommand` instead of a `DepositCommand` or `WithdrawCommand`.
3. `ForeignTransactionCommand#execute()` calls `CurrencyRates#convert()`, which throws a
   `FinNusException` if no rate is set, or if the SGD value is below $0.01 or above
   `Money.MAX_AMOUNT`.
4. `convert()` returns a `ForeignAmount`, a record of the currency code, the amount as typed and
   the rate used. `toSgd()` divides the amount by the rate and rounds it to the nearest cent.
5. The command creates a `Transaction` from the `ForeignAmount`. Its `amount` is the SGD value, so
   the balance, `recent` and `delete` work without any changes.

In the save file, a foreign transaction has three extra fields: the currency code, the amount as
typed and the rate, e.g. `TRANSACTION|WITHDRAWAL|10.00|UmFtZW4=|JPY|1100|110`. SGD-only rows keep
the old format, so existing save files still load. When loading, a row whose SGD amount does not
match its amount and rate is skipped, since the file must have been edited by hand.

#### Design considerations

**Aspect: when the amount is converted**

* **Alternative 1 (current choice):** once, when the transaction is added, and the rate used is
  saved with it.
  * Pros: changing a rate later never changes past transactions or the balance.
  * Cons: each foreign transaction saves three extra values in the save file: the currency code,
    the original amount and the rate.
* **Alternative 2:** every time the balance is calculated, using the current rate.
  * Pros: nothing extra to store.
  * Cons: setting a new rate would silently change past spending and the balance.

**Aspect: which command handles foreign amounts**

* **Alternative 1 (current choice):** a separate `ForeignTransactionCommand`.
  * Pros: the rates are in `TransactionList`, which only a command can reach when it runs, so
    conversion and its errors stay in one place. `DepositCommand` and `WithdrawCommand` are
    unchanged.
  * Cons: one more class, and the confirmation message is built in two places.
* **Alternative 2:** add an optional currency to `DepositCommand` and `WithdrawCommand`.
  * Pros: fewer classes.
  * Cons: both commands would need the same conversion code, and their `execute()` would start
    throwing an exception that every existing caller has to handle.
<!-- @@author -->


## Product scope
### Target user profile

{Describe the target user profile}

### Value proposition

{Describe the value proposition: what problem does it solve?}

## User Stories

|Version| As a ... | I want to ... | So that I can ...|
|--------|----------|---------------|------------------|
|v1.0|new user|see usage instructions|refer to them when I forget how to use the application|
|v2.0|user|find a to-do item by name|locate a to-do without having to go through the entire list|

## Non-Functional Requirements

{Give non-functional requirements}

## Glossary

* *glossary item* - Definition

## Instructions for manual testing

{Give instructions on how to do a manual product testing e.g., how to load sample data to be used for testing}
