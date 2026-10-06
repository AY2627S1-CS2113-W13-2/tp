package finnus.log;

import java.math.BigDecimal;

/** Represents a transaction, storing its type, description and amount */
public class Transaction {

    private String transactionType;
    private String description;
    private BigDecimal amount;

    /** 
     * Creates a transaction without a description
     * 
     * @param transactionType Either deposit or withdrawal
     * @param amount The transaction amount as a BigDecimal value 
     */
    public Transaction(String transactionType, BigDecimal amount) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = "";
    }

    /** 
     * Creates a transaction with a description provided
     * 
     * @param transactionType Either deposit or withdrawal
     * @param amount The transaction amount as a BigDecimal value
     * @param description The description for the transaction
     */
    public Transaction(String transactionType, BigDecimal amount, String description) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
    }

    /** Returns transaction type */
    public String getTransactionType() {
        return transactionType;
    }

    /** Returns transaction amount */
    public BigDecimal getAmount() {
        return amount;
    }

    /** Returns description of transaction */
    public String getDescription() {
        return description;
    }
}
