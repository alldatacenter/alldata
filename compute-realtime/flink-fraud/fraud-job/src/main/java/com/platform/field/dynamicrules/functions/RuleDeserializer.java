package com.platform.field.dynamicrules.functions;

import com.platform.field.dynamicrules.Rule;
import com.platform.field.dynamicrules.Rule.RuleState;
import com.platform.field.dynamicrules.RuleParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

@Slf4j
public class RuleDeserializer extends RichFlatMapFunction<String, Rule> {

  private RuleParser ruleParser;

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    ruleParser = new RuleParser();
  }

  @Override
  public void flatMap(String value, Collector<Rule> out) throws Exception {
    log.info("{}", value);
    try {
      Rule rule = ruleParser.fromString(value);
      if (rule.getRuleState() != RuleState.CONTROL && rule.getRuleId() == null) {
        throw new NullPointerException("ruleId cannot be null: " + rule.toString());
      }
      out.collect(rule);
    } catch (Exception e) {
      log.warn("Failed parsing rule, dropping it:", e);
    }
  }
}
