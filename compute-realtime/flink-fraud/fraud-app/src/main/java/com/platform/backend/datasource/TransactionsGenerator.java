package com.platform.backend.datasource;

import com.platform.backend.datasource.Transaction.PaymentType;
import java.math.BigDecimal;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionsGenerator implements Runnable {

  private static long MAX_PAYEE_ID = 100000;
  private static long MAX_BENEFICIARY_ID = 100000;

  private static double MIN_PAYMENT_AMOUNT = 5d;
  private static double MAX_PAYMENT_AMOUNT = 20d;
  private final Throttler throttler;

  private volatile boolean running = true;
  private Integer maxRecordsPerSecond;

  private Consumer<Transaction> consumer;

  public TransactionsGenerator(Consumer<Transaction> consumer, int maxRecordsPerSecond) {
    this.consumer = consumer;
    this.maxRecordsPerSecond = maxRecordsPerSecond;
    this.throttler = new Throttler(maxRecordsPerSecond);
  }

  public void adjustMaxRecordsPerSecond(long maxRecordsPerSecond) {
    throttler.adjustMaxRecordsPerSecond(maxRecordsPerSecond);
  }

  protected Transaction randomEvent(SplittableRandom rnd) {
    long transactionId = rnd.nextLong(Long.MAX_VALUE);
    long payeeId = rnd.nextLong(MAX_PAYEE_ID);
    long beneficiaryId = rnd.nextLong(MAX_BENEFICIARY_ID);
    double paymentAmountDouble =
        ThreadLocalRandom.current().nextDouble(MIN_PAYMENT_AMOUNT, MAX_PAYMENT_AMOUNT);
    paymentAmountDouble = Math.floor(paymentAmountDouble * 100) / 100;
    BigDecimal paymentAmount = BigDecimal.valueOf(paymentAmountDouble);

    return Transaction.builder()
        .transactionId(transactionId)
        .payeeId(payeeId)
        .beneficiaryId(beneficiaryId)
        .paymentAmount(paymentAmount)
        .paymentType(paymentType(transactionId))
        .eventTime(System.currentTimeMillis())
        .build();
  }

  public Transaction generateOne() {
    return randomEvent(new SplittableRandom());
  }

  private static PaymentType paymentType(long id) {
    int name = (int) (id % 2);
    switch (name) {
      case 0:
        return PaymentType.CRD;
      case 1:
        return PaymentType.CSH;
      default:
        throw new IllegalStateException("");
    }
  }

  @Override
  public final void run() {
    running = true;

    final SplittableRandom rnd = new SplittableRandom();

    while (running) {
      Transaction event = randomEvent(rnd);
      log.debug("{}", event);
      consumer.accept(event);
      try {
        throttler.throttle();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    log.info("Finished run()");
  }

  public final void cancel() {
    running = false;
    log.info("Cancelled");
  }
}
