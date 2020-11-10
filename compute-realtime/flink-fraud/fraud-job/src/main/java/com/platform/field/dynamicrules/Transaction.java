package com.platform.field.dynamicrules;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction implements TimestampAssignable<Long> {
    public long transactionId;
    public long eventTime;
    public long payeeId;
    public long beneficiaryId;
    public BigDecimal paymentAmount;
    public PaymentType paymentType;
    private Long ingestionTimestamp;
    private static transient DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.US)
                    .withZone(ZoneOffset.UTC);

    public enum PaymentType {
        CSH("CSH"),
        CRD("CRD");
        String representation;

        PaymentType(String repr) {
            this.representation = repr;
        }

        public static PaymentType fromString(String representation) {
            for (PaymentType b : PaymentType.values()) {
                if (b.representation.equals(representation)) {
                    return b;
                }
            }
            return null;
        }
    }

    public static Transaction fromString(String line) {
        List<String> tokens = Arrays.asList(line.split(","));
        int numArgs = 7;
        if (tokens.size() != numArgs) {
            throw new RuntimeException(
                    "Invalid transaction: "
                            + line
                            + ". Required number of arguments: "
                            + numArgs
                            + " found "
                            + tokens.size());
        }
        Transaction transaction = new Transaction();
        try {
            Iterator<String> iter = tokens.iterator();
            transaction.transactionId = Long.parseLong(iter.next());
            transaction.eventTime =
                    ZonedDateTime.parse(iter.next(), timeFormatter).toInstant().toEpochMilli();
            transaction.payeeId = Long.parseLong(iter.next());
            transaction.beneficiaryId = Long.parseLong(iter.next());
            transaction.paymentType = PaymentType.fromString(iter.next());
            transaction.paymentAmount = new BigDecimal(iter.next());
            transaction.ingestionTimestamp = Long.parseLong(iter.next());
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Invalid record: " + line, nfe);
        }
        return transaction;
    }

    @Override
    public void assignIngestionTimestamp(Long timestamp) {
        this.ingestionTimestamp = timestamp;
    }
}
