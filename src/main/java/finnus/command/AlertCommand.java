//@@author lololol8484
package finnus.command;

import java.math.BigDecimal;
import java.util.logging.Logger;

import finnus.alert.SpendingAlert;
import finnus.money.Money;
import finnus.transaction.TransactionList;
import finnus.ui.Ui;

/** Displays or updates the two optional, user-configured spending alert thresholds. */
public class AlertCommand extends Command {
    private static final Logger logger = Logger.getLogger(AlertCommand.class.getName());

    private enum Setting {
        SHOW, BALANCE, ALLOWANCE_PERCENTAGE
    }

    private final Setting setting;
    private final Money balanceThreshold;
    private final BigDecimal allowancePercentage;
    private final boolean clear;

    /** Creates a command that displays the current alert settings. */
    public AlertCommand() {
        this(Setting.SHOW, null, null, false);
    }

    private AlertCommand(Setting setting, Money balanceThreshold, BigDecimal allowancePercentage, boolean clear) {
        this.setting = setting;
        this.balanceThreshold = balanceThreshold;
        this.allowancePercentage = allowancePercentage;
        this.clear = clear;
    }

    /** Creates a command that sets the fixed balance threshold. */
    public static AlertCommand setBalance(Money threshold) {
        assert threshold != null && threshold.isPositive() : "Balance threshold must be positive";
        return new AlertCommand(Setting.BALANCE, threshold, null, false);
    }

    /** Creates a command that clears the fixed balance threshold. */
    public static AlertCommand clearBalance() {
        return new AlertCommand(Setting.BALANCE, null, null, true);
    }

    /** Creates a command that sets the allowance percentage threshold. */
    public static AlertCommand setAllowancePercentage(BigDecimal percentage) {
        assert percentage != null && percentage.signum() > 0
                && percentage.compareTo(BigDecimal.valueOf(100)) <= 0
                : "Allowance percentage must be between 1 and 100";
        return new AlertCommand(Setting.ALLOWANCE_PERCENTAGE, null, percentage, false);
    }

    /** Creates a command that clears the allowance percentage threshold. */
    public static AlertCommand clearAllowancePercentage() {
        return new AlertCommand(Setting.ALLOWANCE_PERCENTAGE, null, null, true);
    }

    /** Displays or updates alert settings while preserving the other optional threshold. */
    @Override
    public void execute(TransactionList transactions, Ui ui) {
        assertExecutionContext(transactions, ui);
        SpendingAlert current = transactions.getSpendingAlert();
        if (setting == Setting.SHOW) {
            showSettings(current, ui);
            return;
        }

        Money updatedBalance = setting == Setting.BALANCE
                ? (clear ? null : balanceThreshold) : current.balanceThreshold().orElse(null);
        BigDecimal updatedPercentage = setting == Setting.ALLOWANCE_PERCENTAGE
                ? (clear ? null : allowancePercentage) : current.allowancePercentage().orElse(null);
        transactions.setSpendingAlert(new SpendingAlert(updatedBalance, updatedPercentage));
        showUpdate(ui);
        logger.info("Updated " + setting.name().toLowerCase().replace('_', ' ') + " spending alert setting"
                + (clear ? " (cleared)" : ""));
    }

    /** Shows configured optional thresholds and explains when the allowance percentage applies. */
    private static void showSettings(SpendingAlert settings, Ui ui) {
        String balance = settings.balanceThreshold().map(Money::format).orElse("not set");
        String percentage = settings.allowancePercentage()
                .map(value -> value.stripTrailingZeros().toPlainString() + "%")
                .orElse("not set");
        ui.showMessage("Optional spending alert settings:",
                "Balance threshold: " + balance,
                "Allowance threshold: " + percentage,
                "The allowance threshold applies when an allowance amount is configured.",
                "Automatic alerts for the full allowance, opening balance and $0.00 remain active.");
        logger.fine("Displayed spending alert settings");
    }

    /** Reports the setting that was changed. */
    private void showUpdate(Ui ui) {
        if (setting == Setting.BALANCE) {
            ui.showMessage(clear ? "Balance alert threshold cleared."
                    : "Balance alert threshold set to " + balanceThreshold.format() + ".");
            return;
        }
        String percentage = allowancePercentage == null ? "" : allowancePercentage.toPlainString() + "%";
        ui.showMessage(clear ? "Allowance alert percentage cleared."
                : "Allowance alert percentage set to " + percentage + ".");
    }
}
//@@author
