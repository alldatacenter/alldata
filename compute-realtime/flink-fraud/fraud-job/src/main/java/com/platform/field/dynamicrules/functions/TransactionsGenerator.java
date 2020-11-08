package com.platform.field.dynamicrules.functions;

import com.platform.field.dynamicrules.Transaction;
import com.platform.field.sources.BaseGenerator;

import java.math.BigDecimal;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

public class TransactionsGenerator extends BaseGenerator<Transaction> {

  private static long MAX_PAYEE_ID = 100000;
  private static long MAX_BENEFICIARY_ID = 100000;

  private static double MIN_PAYMENT_AMOUNT = 5d;
  private static double MAX_PAYMENT_AMOUNT = 20d;

  public TransactionsGenerator(int maxRecordsPerSecond) {
    super(maxRecordsPerSecond);
  }

  @Override
  public Transaction randomEvent(SplittableRandom rnd, long id) {
    long transactionId = rnd.nextLong(Long.MAX_VALUE);
    long payeeId = rnd.nextLong(MAX_PAYEE_ID);
    long beneficiaryId = rnd.nextLong(MAX_BENEFICIARY_ID);
    double paymentAmountDouble =
        ThreadLocalRandom.current().nextDouble(MIN_PAYMENT_AMOUNT, MAX_PAYMENT_AMOUNT);
    paymentAmountDouble = Math.floor(paymentAmountDouble * 100) / 100;
    BigDecimal paymentAmount = BigDecimal.valueOf(paymentAmountDouble);

    Transaction transaction =
        Transaction.builder()
            .transactionId(transactionId)
            .payeeId(payeeId)
            .beneficiaryId(beneficiaryId)
            .paymentAmount(paymentAmount)
            .paymentType(paymentType(transactionId))
            .eventTime(System.currentTimeMillis())
            .ingestionTimestamp(System.currentTimeMillis())
            .build();

    return transaction;
  }

  private Transaction.PaymentType paymentType(long id) {
    int name = (int) (id % 2);
    switch (name) {
      case 0:
        return Transaction.PaymentType.CRD;
      case 1:
        return Transaction.PaymentType.CSH;
      default:
        throw new IllegalStateException("");
    }
  }
}
