package com.platform.field.dynamicrules;

import com.platform.field.dynamicrules.accumulators.AverageAccumulator;
import com.platform.field.dynamicrules.accumulators.BigDecimalCounter;
import com.platform.field.dynamicrules.accumulators.BigDecimalMaximum;
import com.platform.field.dynamicrules.accumulators.BigDecimalMinimum;
import java.math.BigDecimal;
import org.apache.flink.api.common.accumulators.SimpleAccumulator;

public class RuleHelper {

  public static SimpleAccumulator<BigDecimal> getAggregator(Rule rule) {
    switch (rule.getAggregatorFunctionType()) {
      case SUM:
        return new BigDecimalCounter();
      case AVG:
        return new AverageAccumulator();
      case MAX:
        return new BigDecimalMaximum();
      case MIN:
        return new BigDecimalMinimum();
      default:
        throw new RuntimeException(
            "Unsupported aggregation function type: " + rule.getAggregatorFunctionType());
    }
  }
}
