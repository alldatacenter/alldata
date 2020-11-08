package com.platform.field.dynamicrules;
import static org.junit.Assert.assertEquals;

import com.platform.field.dynamicrules.Rule.AggregatorFunctionType;
import com.platform.field.dynamicrules.Rule.LimitOperatorType;
import com.platform.field.dynamicrules.Rule.RuleState;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;public class RuleParserTest {@SafeVarargs
  public static <T> List<T> lst(T... ts) {
    return Arrays.asList(ts);
  }@Test
  public void testRuleParsedPlain() throws Exception {
    String ruleString1 = "1,(active),(taxiId&driverId),,(totalFare),(sum),(>),(5),(20)";
    RuleParser ruleParser = new RuleParser();
    Rule rule1 = ruleParser.fromString(ruleString1);assertEquals("ID incorrect", 1, (int) rule1.getRuleId());
    Assert.assertEquals("Rule state incorrect", RuleState.ACTIVE, rule1.getRuleState());
    assertEquals("Key names incorrect", lst("taxiId", "driverId"), rule1.getGroupingKeyNames());
    assertEquals("Unique names incorrect", lst(), rule1.getUnique());
    assertEquals("Cumulative key incorrect", "totalFare", rule1.getAggregateFieldName());
    Assert.assertEquals(
        "Aggregator function incorrect",
        AggregatorFunctionType.SUM,
        rule1.getAggregatorFunctionType());
    Assert.assertEquals(
        "Limit operator incorrect", LimitOperatorType.GREATER, rule1.getLimitOperatorType());
    assertEquals("Limit incorrect", BigDecimal.valueOf(5), rule1.getLimit());
    assertEquals("Window incorrect", 20, (int) rule1.getWindowMinutes());
  }@Test
  public void testRuleParsedJson() throws Exception {
    String ruleString1 =
        "{\n"
            + "  \"ruleId\": 1,\n"
            + "  \"ruleState\": \"ACTIVE\",\n"
            + "  \"groupingKeyNames\": [\"taxiId\", \"driverId\"],\n"
            + "  \"unique\": [],\n"
            + "  \"aggregateFieldName\": \"totalFare\",\n"
            + "  \"aggregatorFunctionType\": \"SUM\",\n"
            + "  \"limitOperatorType\": \"GREATER\",\n"
            + "  \"limit\": 50,\n"
            + "  \"windowMinutes\": 20\n"
            + "}";RuleParser ruleParser = new RuleParser();
    Rule rule1 = ruleParser.fromString(ruleString1);assertEquals("ID incorrect", 1, (int) rule1.getRuleId());
    Assert.assertEquals("Rule state incorrect", RuleState.ACTIVE, rule1.getRuleState());
    assertEquals("Key names incorrect", lst("taxiId", "driverId"), rule1.getGroupingKeyNames());
    assertEquals("Unique names incorrect", lst(), rule1.getUnique());
    assertEquals("Cumulative key incorrect", "totalFare", rule1.getAggregateFieldName());
    Assert.assertEquals(
        "Aggregator function incorrect",
        AggregatorFunctionType.SUM,
        rule1.getAggregatorFunctionType());
    Assert.assertEquals(
        "Limit operator incorrect", LimitOperatorType.GREATER, rule1.getLimitOperatorType());
    assertEquals("Limit incorrect", BigDecimal.valueOf(50), rule1.getLimit());
    assertEquals("Window incorrect", 20, (int) rule1.getWindowMinutes());
  }
}
