package com.platform.field.dynamicrules;

import com.platform.field.dynamicrules.RulesEvaluator.Descriptors;
import com.platform.field.dynamicrules.functions.DynamicAlertFunction;
import com.platform.field.dynamicrules.functions.DynamicKeyFunction;
import com.platform.field.dynamicrules.util.AssertUtils;
import com.platform.field.dynamicrules.util.BroadcastStreamKeyedOperatorTestHarness;
import com.platform.field.dynamicrules.util.BroadcastStreamNonKeyedOperatorTestHarness;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.TestHarnessUtil;
import org.junit.Test;


public class RulesEvaluatorTest {

  @Test
  public void shouldProduceKeyedOutput() throws Exception {
    RuleParser ruleParser = new RuleParser();
    Rule rule1 =
        ruleParser.fromString("1,(active),(paymentType&payeeId),,(totalFare),(SUM),(>),(50),(20)");
    Transaction event1 = Transaction.fromString("1,2013-01-01 00:00:00,1001,1002,CSH,21.5,1");

    try (BroadcastStreamNonKeyedOperatorTestHarness<
            Transaction, Rule, Keyed<Transaction, String, Integer>>
        testHarness =
            BroadcastStreamNonKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicKeyFunction(), Descriptors.rulesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(rule1, 12L));
      testHarness.processElement1(new StreamRecord<>(event1, 15L));

      Queue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      expectedOutput.add(
          new StreamRecord<>(new Keyed<>(event1, "{paymentType=CSH;payeeId=1001}", 1), 15L));

      TestHarnessUtil.assertOutputEquals(
          "Wrong dynamically keyed output", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldStoreRulesInBroadcastStateDuringDynamicKeying() throws Exception {
    RuleParser ruleParser = new RuleParser();
    Rule rule1 = ruleParser.fromString("1,(active),(paymentType),,(totalFare),(SUM),(>),(50),(20)");

    try (BroadcastStreamNonKeyedOperatorTestHarness<
            Transaction, Rule, Keyed<Transaction, String, Integer>>
        testHarness =
            BroadcastStreamNonKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicKeyFunction(), Descriptors.rulesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(rule1, 12L));

      BroadcastState<Integer, Rule> broadcastState =
          testHarness.getBroadcastState(Descriptors.rulesDescriptor);

      Map<Integer, Rule> expectedState = new HashMap<>();
      expectedState.put(rule1.getRuleId(), rule1);

      AssertUtils.assertEquals(broadcastState, expectedState, "Output was not correct.");
    }
  }

  @Test
  public void shouldOutputSimplestAlert() throws Exception {
    RuleParser ruleParser = new RuleParser();
    Rule rule1 =
        ruleParser.fromString("1,(active),(paymentType),,(paymentAmount),(SUM),(>),(20),(20)");

    Transaction event1 = Transaction.fromString("1,2013-01-01 00:00:00,1001,1002,CSH,22,1");
    Transaction event2 = Transaction.fromString("2,2013-01-01 00:00:01,1001,1002,CRD,19,1");
    Transaction event3 = Transaction.fromString("3,2013-01-01 00:00:02,1001,1002,CRD,2,1");

    Keyed<Transaction, String, Integer> keyed1 = new Keyed<>(event1, "CSH", 1);
    Keyed<Transaction, String, Integer> keyed2 = new Keyed<>(event2, "CRD", 1);
    Keyed<Transaction, String, Integer> keyed3 = new Keyed<>(event3, "CRD", 1);

    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Keyed<Transaction, String, Integer>, Rule, Alert>
        testHarness =
            BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicAlertFunction(),
                in -> (in.getKey()),
                null,
                BasicTypeInfo.STRING_TYPE_INFO,
                Descriptors.rulesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(rule1, 12L));

      testHarness.processElement1(new StreamRecord<>(keyed1, 15L));
      testHarness.processElement1(new StreamRecord<>(keyed2, 16L));
      testHarness.processElement1(new StreamRecord<>(keyed3, 17L));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Transaction, BigDecimal> alert1 =
          new Alert<>(rule1.getRuleId(), rule1, "CSH", event1, BigDecimal.valueOf(22));
      Alert<Transaction, BigDecimal> alert2 =
          new Alert<>(rule1.getRuleId(), rule1, "CRD", event3, BigDecimal.valueOf(21));

      expectedOutput.add(new StreamRecord<>(alert1, 15L));
      expectedOutput.add(new StreamRecord<>(alert2, 17L));

      TestHarnessUtil.assertOutputEquals(
          "Output was not correct.", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldHandleSameTimestampEventsCorrectly() throws Exception {
    RuleParser ruleParser = new RuleParser();
    Rule rule1 =
        ruleParser.fromString("1,(active),(paymentType),,(paymentAmount),(SUM),(>),(20),(20)");

    Transaction event1 = Transaction.fromString("1,2013-01-01 00:00:00,1001,1002,CSH,19,1");

    Transaction event2 = Transaction.fromString("2,2013-01-01 00:00:00,1002,1003,CSH,2,1");

    Keyed<Transaction, String, Integer> keyed1 = new Keyed<>(event1, "CSH", 1);
    Keyed<Transaction, String, Integer> keyed2 = new Keyed<>(event2, "CSH", 1);

    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Keyed<Transaction, String, Integer>, Rule, Alert>
        testHarness =
            BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicAlertFunction(),
                in -> (in.getKey()),
                null,
                BasicTypeInfo.STRING_TYPE_INFO,
                Descriptors.rulesDescriptor)) {

      testHarness.processElement2(new StreamRecord<>(rule1, 12L));

      testHarness.processElement1(new StreamRecord<>(keyed1, 15L));
      testHarness.processElement1(new StreamRecord<>(keyed2, 16L));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Transaction, BigDecimal> alert1 =
          new Alert<>(rule1.getRuleId(), rule1, "CSH", event2, new BigDecimal(21));

      expectedOutput.add(new StreamRecord<>(alert1, 16L));

      TestHarnessUtil.assertOutputEquals(
          "Output was not correct.", expectedOutput, testHarness.getOutput());
    }
  }

  @Test
  public void shouldCleanupStateBasedOnWatermarks() throws Exception {
    RuleParser ruleParser = new RuleParser();
    Rule rule1 =
        ruleParser.fromString("1,(active),(paymentType),,(paymentAmount),(SUM),(>),(10),(4)");

    Transaction event1 = Transaction.fromString("1,2013-01-01 00:01:00,1001,1002,CSH,3,1");

    Transaction event2 = Transaction.fromString("2,2013-01-01 00:02:00,1003,1004,CSH,3,1");

    Transaction event3 = Transaction.fromString("3,2013-01-01 00:03:00,1005,1006,CSH,5,1");

    Transaction event4 = Transaction.fromString("4,2013-01-01 00:06:00,1007,1008,CSH,3,1");

    Keyed<Transaction, String, Integer> keyed1 = new Keyed<>(event1, "CSH", 1);
    Keyed<Transaction, String, Integer> keyed2 = new Keyed<>(event2, "CSH", 1);
    Keyed<Transaction, String, Integer> keyed3 = new Keyed<>(event3, "CSH", 1);
    Keyed<Transaction, String, Integer> keyed4 = new Keyed<>(event4, "CSH", 1);

    try (BroadcastStreamKeyedOperatorTestHarness<
            String, Keyed<Transaction, String, Integer>, Rule, Alert>
        testHarness =
            BroadcastStreamKeyedOperatorTestHarness.getInitializedTestHarness(
                new DynamicAlertFunction(),
                in -> (in.getKey()),
                null,
                BasicTypeInfo.STRING_TYPE_INFO,
                Descriptors.rulesDescriptor)) {

      //      long halfAMinuteMillis = 30 * 1000l;
      long watermarkDelay = 2 * 60 * 1000l;

      testHarness.processElement2(new StreamRecord<>(rule1, 1L));

      testHarness.processElement1(toStreamRecord(keyed1));
      testHarness.watermark(event1.getEventTime() - watermarkDelay);

      testHarness.processElement1(toStreamRecord(keyed2));
      testHarness.watermark(event2.getEventTime() - watermarkDelay);

      testHarness.processElement1(toStreamRecord(keyed4));
      testHarness.watermark(event4.getEventTime() - watermarkDelay);

      // Cleaning up on per-event fixed basis had caused event4 to delete event1 from the state,
      // hence event3 would not have fired. We expect event3 to fire.
      testHarness.processElement1(toStreamRecord(keyed3));

      ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();
      Alert<Transaction, BigDecimal> alert1 =
          new Alert<>(rule1.getRuleId(), rule1, "CSH", event3, new BigDecimal(11));

      expectedOutput.add(new StreamRecord<>(alert1, event3.getEventTime()));

      TestHarnessUtil.assertOutputEquals(
          "Output was not correct.", expectedOutput, filterOutWatermarks(testHarness.getOutput()));
    }
  }

  private StreamRecord<Keyed<Transaction, String, Integer>> toStreamRecord(
      Keyed<Transaction, String, Integer> keyed) {
    return new StreamRecord<>(keyed, keyed.getWrapped().getEventTime());
  }

  private Queue<Object> filterOutWatermarks(Queue<Object> in) {
    Queue<Object> out = new LinkedList<>();
    for (Object o : in) {
      if (!(o instanceof Watermark)) {
        out.add(o);
      }
    }
    return out;
  }
}
