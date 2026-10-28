# Developer Guide

## Acknowledgements

{list here sources of all reused/adapted ideas, code, documentation, and third-party libraries -- include links to the original source as well}

## Design & implementation

{Describe the design and implementation of the product. Use UML diagrams and short code snippets where applicable.}

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
  (`DEPOSIT` or `WITHDRAWAL`), a positive `BigDecimal` amount and a description.
* `TransactionList` holds all transactions in the order they were added.

#### How a withdrawal is processed

Given below is what happens when the user enters `withdraw 4.50 d/Lunch`.

1. `FinNUS#run()` reads the line through `Ui` and passes it to `Parser#parse()`.
2. `Parser#parse()` reads the command word `withdraw` and calls `parseWithdraw("4.50 d/Lunch")`.
3. `splitAmountAndDescription()` splits the arguments into the amount `"4.50"` and the
   description `"Lunch"`. The `d/` prefix only counts when it starts a word, so text such as
   `and/or` inside a description is not mistaken for a prefix. The amount and `d/DESCRIPTION`
   may be given in either order, e.g. `withdraw d/Lunch 4.50`.
4. `parseAmount()` checks the amount and converts it to a `BigDecimal`. For a withdrawal,
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
* is not more than `Parser.MAX_AMOUNT` (1,000,000), which catches typos such as extra zeros.

If any rule fails, the parser throws a `FinNusException`. The main loop catches it and shows the
message to the user, so invalid input never stops the app.

#### Design considerations

**Aspect: how amounts are stored**

* **Alternative 1 (current choice):** `BigDecimal`.
  * Pros: stores decimal amounts exactly, so the balance never drifts.
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

<!-- @@author chancheese -->
### Recording transactions in foreign currencies

Adding `c/CURRENCY` to `deposit` or `withdraw` (e.g. `withdraw 1100 d/Ramen c/JPY`) records an
amount typed in another currency. It is converted to SGD with the rate set by `rate`, and the
original amount is kept so it can be shown later, e.g. `[Withdrawal] -$10.00 (Ramen) [JPY 1100]`.

#### How it works

The sequence diagram below shows what happens when the user enters `withdraw 1100 d/Ramen c/JPY`.
Logging, console output, input validation and saving the ledger afterwards are left out.

![Sequence diagram of a foreign-currency withdrawal](images/ForeignTransactionSequenceDiagram.png)

1. `Parser#extractCurrency()` removes the `c/CURRENCY` part from the arguments, so the rest is
   split into amount and description exactly as before. `c/SGD` counts as no currency.
2. If a currency was given, `parseForeignAmount()` checks the amount and the parser returns a
   `ForeignTransactionCommand` instead of a `DepositCommand` or `WithdrawCommand`.
3. `ForeignTransactionCommand#execute()` calls `CurrencyRates#convert()`, which throws a
   `FinNusException` if no rate is set for the currency.
4. Otherwise, `convert()` creates a `ForeignAmount`, a record of the currency code, the amount as
   typed and the rate used, and calls its `toSgd()`, which divides the amount by the rate and
   rounds it to the nearest cent. If the result is $0.00 or above `Money.MAX_AMOUNT`
   ($1,000,000), `convert()` throws a `FinNusException`; otherwise it returns the `ForeignAmount`.
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
  * Cons: each foreign transaction stores two extra values.
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
