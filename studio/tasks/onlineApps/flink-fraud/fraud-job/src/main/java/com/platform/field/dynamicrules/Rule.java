package com.platform.field.dynamicrules;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.flink.api.common.time.Time;

@EqualsAndHashCode
@ToString
@Data
public class Rule {

  private Integer ruleId;
  private RuleState ruleState;
  private List<String> groupingKeyNames; // aggregation
  private List<String> unique;
  private String aggregateFieldName;
  private AggregatorFunctionType aggregatorFunctionType;
  private LimitOperatorType limitOperatorType;
  private BigDecimal limit;
  private Integer windowMinutes;
  private ControlType controlType;

  public Long getWindowMillis() {
    return Time.minutes(this.windowMinutes).toMilliseconds();
  }

  public boolean apply(BigDecimal comparisonValue) {
    switch (limitOperatorType) {
      case EQUAL:
        return comparisonValue.compareTo(limit) == 0;
      case NOT_EQUAL:
        return comparisonValue.compareTo(limit) != 0;
      case GREATER:
        return comparisonValue.compareTo(limit) > 0;
      case LESS:
        return comparisonValue.compareTo(limit) < 0;
      case LESS_EQUAL:
        return comparisonValue.compareTo(limit) <= 0;
      case GREATER_EQUAL:
        return comparisonValue.compareTo(limit) >= 0;
      default:
        throw new RuntimeException("Unknown limit operator type: " + limitOperatorType);
    }
  }

  public long getWindowStartFor(Long timestamp) {
    Long ruleWindowMillis = getWindowMillis();
    return (timestamp - ruleWindowMillis);
  }

  public enum AggregatorFunctionType {
    SUM,
    AVG,
    MIN,
    MAX
  }

  public enum LimitOperatorType {
    EQUAL("="),
    NOT_EQUAL("!="),
    GREATER_EQUAL(">="),
    LESS_EQUAL("<="),
    GREATER(">"),
    LESS("<");

    String operator;

    LimitOperatorType(String operator) {
      this.operator = operator;
    }

    public static LimitOperatorType fromString(String text) {
      for (LimitOperatorType b : LimitOperatorType.values()) {
        if (b.operator.equals(text)) {
          return b;
        }
      }
      return null;
    }
  }

  public enum RuleState {
    ACTIVE,
    PAUSE,
    DELETE,
    CONTROL
  }

  public enum ControlType {
    CLEAR_STATE_ALL,
    CLEAR_STATE_ALL_STOP,
    DELETE_RULES_ALL,
    EXPORT_RULES_CURRENT
  }
}
