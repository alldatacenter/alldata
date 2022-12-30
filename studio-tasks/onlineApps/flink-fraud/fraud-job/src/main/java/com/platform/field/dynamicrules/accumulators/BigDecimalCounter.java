package com.platform.field.dynamicrules.accumulators;

import java.math.BigDecimal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.accumulators.SimpleAccumulator;

@PublicEvolving
public class BigDecimalCounter implements SimpleAccumulator<BigDecimal> {

  private static final long serialVersionUID = 1L;

  private BigDecimal localValue = BigDecimal.ZERO;

  public BigDecimalCounter() {}

  public BigDecimalCounter(BigDecimal value) {
    this.localValue = value;
  }

  // ------------------------------------------------------------------------
  //  Accumulator
  // ------------------------------------------------------------------------

  @Override
  public void add(BigDecimal value) {
    localValue = localValue.add(value);
  }

  @Override
  public BigDecimal getLocalValue() {
    return localValue;
  }

  @Override
  public void merge(Accumulator<BigDecimal, BigDecimal> other) {
    localValue = localValue.add(other.getLocalValue());
  }

  @Override
  public void resetLocal() {
    this.localValue = BigDecimal.ZERO;
  }

  @Override
  public BigDecimalCounter clone() {
    BigDecimalCounter result = new BigDecimalCounter();
    result.localValue = localValue;
    return result;
  }

  // ------------------------------------------------------------------------
  //  Utilities
  // ------------------------------------------------------------------------

  @Override
  public String toString() {
    return "BigDecimalCounter " + this.localValue;
  }
}
