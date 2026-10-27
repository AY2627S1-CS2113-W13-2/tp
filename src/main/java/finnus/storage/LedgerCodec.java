package finnus.storage;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import finnus.currency.ForeignAmount;
import finnus.money.Money;
import finnus.storage.LedgerSnapshot.StoredTransaction;
import finnus.transaction.TransactionType;

/**
 * Encodes versioned UTF-8 records and recovers valid rows without filesystem or UI access.
 */
public class LedgerCodec {
    private static final Logger logger = Logger.getLogger(LedgerCodec.class.getName());
    private static final String HEADER = "FINNUS|1";
    private static final String SETUP_PENDING = "SETUP_PENDING";
    private static final String OPENING = "OPENING";
    private static final String RATE = "RATE";
    private static final String TRANSACTION = "TRANSACTION";
    //@@author chancheese
    private static final int SGD_TRANSACTION_FIELDS = 4;
    private static final int FOREIGN_TRANSACTION_FIELDS = 7;
    //@@author

    /**
     * Distinguishes damaged data from a newer format that must not be overwritten.
     */
    public enum Status {
        /** The save file exists, but first-run setup has not been confirmed. */
        SETUP_PENDING,
        /** Every non-blank record was valid. */
        CLEAN,
        /** Some records were recovered, but at least one warning was produced. */
        PARTIAL,
        /** No usable records could be recovered. */
        UNRECOVERABLE,
        /** The file declares a format version this application does not support. */
        UNSUPPORTED
    }

    /**
     * Carries recovered values and safe diagnostics, without including raw financial rows.
     */
    public record DecodeResult(LedgerSnapshot snapshot, List<String> warnings, Status status) {
        /**
         * Prevents subsequent mutation of the warning list.
         */
        public DecodeResult {
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * Produces deterministic records with Base64 descriptions and a final newline.
     */
    public String encode(LedgerSnapshot snapshot) {
        assert snapshot != null : "Cannot encode a null ledger snapshot";
        StringBuilder text = new StringBuilder(HEADER).append('\n');
        text.append(OPENING).append('|').append(snapshot.openingBalance().toPlainString()).append('\n');
        new TreeMap<>(snapshot.rates()).forEach((code, rate) -> text.append(RATE).append('|')
                .append(code).append('|').append(rate.toPlainString()).append('\n'));
        for (StoredTransaction transaction : snapshot.transactions()) {
            String description = Base64.getEncoder().encodeToString(
                    transaction.description().getBytes(StandardCharsets.UTF_8));
            text.append(TRANSACTION).append('|').append(transaction.type()).append('|')
                    .append(transaction.amount().toPlainString()).append('|').append(description);
            //@@author chancheese
            ForeignAmount foreign = transaction.foreignAmount();
            if (foreign != null) {
                text.append('|').append(foreign.currencyCode()).append('|')
                        .append(foreign.amount().toPlainString()).append('|').append(foreign.rate().toPlainString());
            }
            //@@author
            text.append('\n');
        }
        String encoded = text.toString();
        assert encoded.startsWith(HEADER + '\n') && encoded.endsWith("\n")
                : "Encoded ledger must have a header and final newline";
        return encoded;
    }

    /** Encodes the persistent state used while first-run setup is incomplete. */
    public String encodeSetupPending() {
        return HEADER + '\n' + SETUP_PENDING + '\n';
    }

    /**
     * Recovers valid records; the first valid opening balance or currency record wins.
     * Blank lines are ignored and an optional UTF-8 byte-order mark is accepted.
     */
    public DecodeResult decode(String text) {
        assert text != null : "Cannot decode null storage text";
        List<String> lines = text.replaceFirst("^\uFEFF", "").lines().toList();
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            boolean newer = !lines.isEmpty() && lines.getFirst().matches("FINNUS\\|[0-9]+");
            return new DecodeResult(LedgerSnapshot.empty(), List.of("Invalid or unsupported storage header."),
                    newer ? Status.UNSUPPORTED : Status.UNRECOVERABLE);
        }
        if (isSetupPending(lines)) {
            return new DecodeResult(LedgerSnapshot.empty(), List.of(), Status.SETUP_PENDING);
        }
        Records records = new Records();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isBlank()) {
                continue;
            }
            try {
                parseRecord(lines.get(index), records);
                records.validRecords++;
            } catch (IllegalArgumentException e) {
                logger.fine("Skipped invalid storage row " + (index + 1) + ": " + e.getMessage());
                records.warnings.add("Skipped row " + (index + 1) + ": " + e.getMessage() + ".");
            }
        }
        return records.result();
    }

    /** Checks whether the only non-blank record after the header marks interrupted setup. */
    private boolean isSetupPending(List<String> lines) {
        List<String> records = lines.subList(1, lines.size()).stream()
                .filter(line -> !line.isBlank())
                .toList();
        return records.size() == 1 && SETUP_PENDING.equals(records.getFirst().trim());
    }

    /** Dispatches one delimited line to the parser for its record type. */
    private void parseRecord(String line, Records records) {
        String[] fields = line.split("\\|", -1);
        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].trim();
        }
        switch (fields[0]) {
        case OPENING -> parseOpening(fields, records);
        case RATE -> parseRate(fields, records);
        case TRANSACTION -> parseTransaction(fields, records);
        default -> throw new IllegalArgumentException("Unknown record type");
        }
    }

    /** Adds the first valid opening balance, rejecting duplicate records. */
    private void parseOpening(String[] fields, Records records) {
        requireFields(fields, 2);
        Money amount = parseMoney(fields[1]);
        if (records.hasOpening) {
            throw new IllegalArgumentException("Duplicate opening balance");
        }
        LedgerSnapshot.validateOpeningBalance(amount);
        records.opening = amount;
        records.hasOpening = true;
    }

    /** Validates and adds a currency rate unless a record for that currency already exists. */
    private void parseRate(String[] fields, Records records) {
        requireFields(fields, 3);
        BigDecimal rate = parseDecimal(fields[2]);
        LedgerSnapshot.validateRate(fields[1], rate);
        if (records.rates.containsKey(fields[1])) {
            throw new IllegalArgumentException("Duplicate currency record");
        }
        records.rates.put(fields[1], rate);
    }

    /**
     * Validates and appends one transaction record. A transaction typed in another currency
     * has three extra fields: the currency code, the amount as typed, and the rate used.
     */
    private void parseTransaction(String[] fields, Records records) {
        if (fields.length != SGD_TRANSACTION_FIELDS && fields.length != FOREIGN_TRANSACTION_FIELDS) {
            throw new IllegalArgumentException("Incorrect field count");
        }
        TransactionType type;
        try {
            type = TransactionType.valueOf(fields[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type");
        }
        //@@author chancheese
        ForeignAmount foreign = fields.length == FOREIGN_TRANSACTION_FIELDS
                ? parseForeignAmount(fields[4], fields[5], fields[6]) : null;
        //@@author
        records.transactions.add(new StoredTransaction(type, parseMoney(fields[2]), decodeDescription(fields[3]),
                foreign));
    }

    //@@author chancheese
    /**
     * Validates the foreign-currency fields of a transaction before building the
     * assertion-checked {@link ForeignAmount}.
     */
    private ForeignAmount parseForeignAmount(String code, String amountText, String rateText) {
        BigDecimal amount = parseDecimal(amountText);
        BigDecimal rate = parseDecimal(rateText);
        LedgerSnapshot.validateRate(code, rate);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Foreign amount must be positive");
        }
        return new ForeignAmount(code, amount, rate);
    }
    //@@author

    /** Decodes a Base64 description and rejects malformed or invalid UTF-8 data. */
    private String decodeDescription(String text) {
        try {
            byte[] bytes = Base64.getDecoder().decode(text);
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (IllegalArgumentException | CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid description encoding");
        }
    }

    /** Parses a plain decimal value, excluding exponent notation and other formats. */
    private BigDecimal parseDecimal(String text) {
        if (!text.matches("-?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Invalid decimal value");
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid decimal value");
        }
    }

    /** Parses an exact monetary value without applying display rounding. */
    private Money parseMoney(String text) {
        return Money.of(parseDecimal(text));
    }

    /** Rejects a record whose field count differs from its format. */
    private void requireFields(String[] fields, int expected) {
        if (fields.length != expected) {
            throw new IllegalArgumentException("Incorrect field count");
        }
    }

    /**
     * Accumulates valid records before constructing an immutable, validated snapshot.
     */
    private static class Records {
        private Money opening = Money.ZERO;
        private boolean hasOpening;
        private int validRecords;
        private final List<StoredTransaction> transactions = new ArrayList<>();
        private final Map<String, BigDecimal> rates = new TreeMap<>();
        private final List<String> warnings = new ArrayList<>();

        private DecodeResult result() {
            if (!hasOpening) {
                warnings.add("Opening balance is missing or invalid; using zero. Balances may be incomplete.");
            }
            Status status = validRecords == 0 ? Status.UNRECOVERABLE
                    : warnings.isEmpty() ? Status.CLEAN : Status.PARTIAL;
            LedgerSnapshot snapshot = new LedgerSnapshot(opening, transactions, rates);
            assert snapshot != null : "Decoded ledger snapshot must exist";
            logger.fine("Decoded ledger with status " + status + " and " + validRecords + " valid rows");
            return new DecodeResult(snapshot, warnings, status);
        }
    }
}
