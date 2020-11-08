package com.platform.field.dynamicrules;

import com.platform.field.dynamicrules.Rule.AggregatorFunctionType;
import com.platform.field.dynamicrules.Rule.LimitOperatorType;
import com.platform.field.dynamicrules.Rule.RuleState;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

public class RuleParser {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public Rule fromString(String line) throws IOException {
    if (line.length() > 0 && '{' == line.charAt(0)) {
      return parseJson(line);
    } else {
      return parsePlain(line);
    }
  }

  private Rule parseJson(String ruleString) throws IOException {
    return objectMapper.readValue(ruleString, Rule.class);
  }

  private static Rule parsePlain(String ruleString) throws IOException {
    List<String> tokens = Arrays.asList(ruleString.split(","));
    if (tokens.size() != 9) {
      throw new IOException("Invalid rule (wrong number of tokens): " + ruleString);
    }

    Iterator<String> iter = tokens.iterator();
    Rule rule = new Rule();

    rule.setRuleId(Integer.parseInt(stripBrackets(iter.next())));
    rule.setRuleState(RuleState.valueOf(stripBrackets(iter.next()).toUpperCase()));
    rule.setGroupingKeyNames(getNames(iter.next()));
    rule.setUnique(getNames(iter.next()));
    rule.setAggregateFieldName(stripBrackets(iter.next()));
    rule.setAggregatorFunctionType(
        AggregatorFunctionType.valueOf(stripBrackets(iter.next()).toUpperCase()));
    rule.setLimitOperatorType(LimitOperatorType.fromString(stripBrackets(iter.next())));
    rule.setLimit(new BigDecimal(stripBrackets(iter.next())));
    rule.setWindowMinutes(Integer.parseInt(stripBrackets(iter.next())));

    return rule;
  }

  private static String stripBrackets(String expression) {
    return expression.replaceAll("[()]", "");
  }

  private static List<String> getNames(String expression) {
    String keyNamesString = expression.replaceAll("[()]", "");
    if (!"".equals(keyNamesString)) {
      String[] tokens = keyNamesString.split("&", -1);
      return Arrays.asList(tokens);
    } else {
      return new ArrayList<>();
    }
  }
}
