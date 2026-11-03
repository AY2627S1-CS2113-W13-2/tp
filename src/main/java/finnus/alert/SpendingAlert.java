package finnus.alert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import finnus.allowance.Allowance;
import finnus.currency.ForeignAmount;
import finnus.money.Money;
import finnus.transaction.Transaction;
import finnus.transaction.TransactionList;
import finnus.transaction.TransactionType;
import finnus.ui.Confirmation;
import finnus.ui.Confirmation.Result;
import finnus.ui.Ui;

//@@author lololol8484
/** Stores optional alert settings and finds spending thresholds crossed by a proposed withdrawal. */
public final class SpendingAlert {
    private static final Logger logger = Logger.getLogger(SpendingAlert.class.getName());
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final Money balanceThreshold;
    private final BigDecimal allowancePercentage;

    /** Creates alert settings; either optional setting may be absent. */
    public SpendingAlert(Money balanceThreshold, BigDecimal allowancePercentage) {
        assert balanceThreshold == null || (balanceThreshold.isPositive()
                && balanceThreshold.compareTo(Money.MAX_AMOUNT) <= 0)
                : "Balance alert threshold must be positive and within the supported range";
        assert allowancePercentage == null || (allowancePercentage.signum() > 0
                && allowancePercentage.compareTo(ONE_HUNDRED) <= 0)
                : "Allowance alert percentage must be between 0 and 100";
        this.balanceThreshold = balanceThreshold;
        this.allowancePercentage = allowancePercentage;
    }

    /** Returns the optional fixed balance threshold. */
    public Optional<Money> balanceThreshold() {
        return Optional.ofNullable(balanceThreshold);
    }

    /** Returns the optional allowance percentage threshold. */
    public Optional<BigDecimal> allowancePercentage() {
        return Optional.ofNullable(allowancePercentage);
    }

    /**
     * Lists each configured or fixed threshold crossed by adding {@code proposedWithdrawal}.
     * A threshold already exceeded before the proposed entry is not reported again.
     */
    public List<String> crossedBy(TransactionList transactions, Transaction proposedWithdrawal) {
        return crossedBy(transactions, null, proposedWithdrawal);
    }

    /** Compares the real ledger with a proposed addition or replacement, without mutating it. */
    private List<String> crossedBy(TransactionList transactions, Transaction original,
            Transaction proposedWithdrawal) {
        assert transactions != null : "Transaction list must not be null";
        assert proposedWithdrawal != null && proposedWithdrawal.getType() == TransactionType.WITHDRAWAL
                : "A spending alert requires a proposed withdrawal";
        List<String> alerts = new ArrayList<>();
        Money balanceBefore = transactions.getBalance();
        Money balanceAfter = balanceBefore.add(proposedWithdrawal.getSignedAmount());
        if (original != null) {
            balanceAfter = balanceAfter.add(original.getAmount());
        }
        if (balanceThreshold != null && crossesBelow(balanceBefore, balanceAfter, balanceThreshold)) {
            alerts.add("Balance would fall below your alert threshold of " + balanceThreshold.format() + ".");
        }
        addAllowanceAlerts(alerts, transactions, original, proposedWithdrawal);
        if (crossesBelow(balanceBefore, balanceAfter, transactions.getOpeningBalance())) {
            alerts.add("Balance would fall below your opening balance of "
                    + transactions.getOpeningBalance().format() + ".");
        }
        if (crossesBelow(balanceBefore, balanceAfter, Money.ZERO)) {
            alerts.add("Balance would fall below $0.00.");
        }
        logger.fine("Checked proposed withdrawal against spending thresholds; crossed: " + alerts.size());
        return List.copyOf(alerts);
    }

    /** Displays crossed thresholds and returns whether the user explicitly approved the withdrawal. */
    public boolean confirmWithdrawal(TransactionList transactions, Transaction proposedWithdrawal, Ui ui) {
        return confirmAlerts(crossedBy(transactions, proposedWithdrawal), ui,
                "Proceed with this withdrawal? [yes/no]: ", "Withdrawal cancelled.");
    }

    /**
     * Requests confirmation for thresholds newly crossed by replacing a withdrawal.
     * The original entry is removed only from the calculation, leaving the ledger untouched until approval.
     *
     * @param transactions The ledger before the edit.
     * @param index The zero-based position of the withdrawal being replaced.
     * @param replacement The validated replacement withdrawal, including its SGD amount and date.
     * @param ui The interface used for alert messages and confirmation.
     * @return Whether the edit may proceed; declining or ending input cancels it.
     */
    public boolean confirmEdit(TransactionList transactions, int index, Transaction replacement, Ui ui) {
        Transaction original = transactions.get(index);
        assert original.getType() == TransactionType.WITHDRAWAL : "Only withdrawal edits require spending alerts";
        return confirmAlerts(crossedBy(transactions, original, replacement), ui,
                "Proceed with this edit? [yes/no]: ", "Edit cancelled.");
    }

    /** Shares confirmation behavior while keeping cancellation messages specific to the operation. */
    private boolean confirmAlerts(List<String> alerts, Ui ui, String prompt, String cancellationMessage) {
        assert ui != null : "UI must not be null";
        if (alerts.isEmpty()) {
            return true;
        }
        List<String> message = new ArrayList<>();
        message.add("Spending alert:");
        message.addAll(alerts);
        ui.showMessage(message);
        Result result = Confirmation.ask(ui, prompt);
        boolean approved = result == Result.YES;
        if (!approved) {
            ui.showMessage(cancellationMessage);
            logger.info(cancellationMessage + " Spending alert confirmation: " + result);
        }
        return approved;
    }

    private void addAllowanceAlerts(List<String> alerts, TransactionList transactions, Transaction original,
            Transaction proposedWithdrawal) {
        // An undated legacy entry cannot be assigned to an allowance cycle, even during an edit.
        if (proposedWithdrawal.getDate().isEmpty()) {
            return;
        }
        Optional<Allowance> allowanceSetting = transactions.getAllowance();
        if (allowanceSetting.isEmpty() || !allowanceSetting.get().hasAmount()) {
            return;
        }
        Allowance allowance = allowanceSetting.get();
        Optional<Money> allowanceAmount = allowanceAmountInSgd(allowance, transactions);
        if (allowanceAmount.isEmpty()) {
            return;
        }
        LocalDate date = proposedWithdrawal.getDate().orElseThrow();
        LocalDate cycleStart = allowance.cycleStart(date);
        LocalDate nextCycleStart = allowance.nextDate(date);
        Money spentBefore = Money.ZERO;
        for (Transaction transaction : transactions.getAll()) {
            if (transaction.getType() == TransactionType.WITHDRAWAL
                    && isInCycle(transaction, cycleStart, nextCycleStart)) {
                spentBefore = spentBefore.add(transaction.getAmount());
            }
        }
        Money spentAfter = spentBefore.add(proposedWithdrawal.getAmount());
        if (original != null && isInCycle(original, cycleStart, nextCycleStart)) {
            spentAfter = spentAfter.add(original.getAmount().negate());
        }
        Money fullAllowance = allowanceAmount.get();
        if (allowancePercentage != null) {
            Money percentageLimit = Money.of(fullAllowance.toBigDecimal()
                    .multiply(allowancePercentage).divide(ONE_HUNDRED));
            if (crossesAbove(spentBefore, spentAfter, percentageLimit)) {
                alerts.add("Withdrawals would exceed " + allowancePercentage.stripTrailingZeros().toPlainString()
                        + "% of your allowance (" + percentageLimit.format() + ").");
            }
        }
        if (crossesAbove(spentBefore, spentAfter, fullAllowance)) {
            alerts.add("Withdrawals would exceed 100% of your allowance (" + fullAllowance.format() + ").");
        }
    }

    /** Uses the replacement's cycle for both totals, including edits that move spending between cycles. */
    private static boolean isInCycle(Transaction transaction, LocalDate start, LocalDate nextStart) {
        return transaction.getDate().filter(day -> !day.isBefore(start) && day.isBefore(nextStart)).isPresent();
    }

    private static Optional<Money> allowanceAmountInSgd(Allowance allowance, TransactionList transactions) {
        if (allowance.currencyCode() == null) {
            return Optional.of(Money.of(allowance.amount()));
        }
        if (!transactions.getRates().hasRate(allowance.currencyCode())) {
            logger.fine("Allowance percentage alert skipped because its currency has no exchange rate");
            return Optional.empty();
        }
        return Optional.of(new ForeignAmount(allowance.currencyCode(), allowance.amount(),
                transactions.getRates().getRate(allowance.currencyCode())).toSgd());
    }

    private static boolean crossesAbove(Money before, Money after, Money threshold) {
        return before.compareTo(threshold) <= 0 && after.compareTo(threshold) > 0;
    }

    private static boolean crossesBelow(Money before, Money after, Money threshold) {
        return before.compareTo(threshold) >= 0 && after.compareTo(threshold) < 0;
    }

    /** Keeps alert settings value-based when snapshots are compared for persistence changes. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SpendingAlert spendingAlert)) {
            return false;
        }
        return Objects.equals(balanceThreshold, spendingAlert.balanceThreshold)
                && Objects.equals(allowancePercentage, spendingAlert.allowancePercentage);
    }

    /** Matches the value-based equality used by ledger snapshots. */
    @Override
    public int hashCode() {
        return Objects.hash(balanceThreshold, allowancePercentage);
    }
}
//@@author
