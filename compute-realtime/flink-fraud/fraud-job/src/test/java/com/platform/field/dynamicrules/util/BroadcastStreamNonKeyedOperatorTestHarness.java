package com.platform.field.dynamicrules.util;
import java.util.Arrays;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.runtime.checkpoint.OperatorSubtaskState;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.operators.co.CoBroadcastWithNonKeyedOperator;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.AbstractStreamOperatorTestHarness;
import org.apache.flink.util.Preconditions;

public class BroadcastStreamNonKeyedOperatorTestHarness<IN1, IN2, OUT>
    extends AbstractStreamOperatorTestHarness<OUT> {private final CoBroadcastWithNonKeyedOperator<IN1, IN2, OUT> twoInputOperator;public BroadcastStreamNonKeyedOperatorTestHarness(
      CoBroadcastWithNonKeyedOperator<IN1, IN2, OUT> operator) throws Exception {
    this(operator, 1, 1, 0);
  }public BroadcastStreamNonKeyedOperatorTestHarness(
      CoBroadcastWithNonKeyedOperator<IN1, IN2, OUT> operator,
      int maxParallelism,
      int numSubtasks,
      int subtaskIndex)
      throws Exception {
    super(operator, maxParallelism, numSubtasks, subtaskIndex);this.twoInputOperator = operator;
  }public void processElement1(StreamRecord<IN1> element) throws Exception {
    twoInputOperator.setKeyContextElement1(element);
    twoInputOperator.processElement1(element);
  }public void processElement2(StreamRecord<IN2> element) throws Exception {
    twoInputOperator.setKeyContextElement2(element);
    twoInputOperator.processElement2(element);
  }public void processWatermark1(Watermark mark) throws Exception {
    twoInputOperator.processWatermark1(mark);
  }public void processWatermark2(Watermark mark) throws Exception {
    twoInputOperator.processWatermark2(mark);
  }public <K, V> BroadcastState<K, V> getBroadcastState(MapStateDescriptor<K, V> stateDescriptor)
      throws Exception {
    return twoInputOperator.getOperatorStateBackend().getBroadcastState(stateDescriptor);
  }public static <IN1, IN2, OUT>
      BroadcastStreamNonKeyedOperatorTestHarness<IN1, IN2, OUT> getInitializedTestHarness(
          final BroadcastProcessFunction<IN1, IN2, OUT> function,
          final MapStateDescriptor<?, ?>... descriptors)
          throws Exception {return getInitializedTestHarness(function, 1, 1, 0, descriptors);
  }public static <IN1, IN2, OUT>
      BroadcastStreamNonKeyedOperatorTestHarness<IN1, IN2, OUT> getInitializedTestHarness(
          final BroadcastProcessFunction<IN1, IN2, OUT> function,
          final int maxParallelism,
          final int numTasks,
          final int taskIdx,
          final MapStateDescriptor<?, ?>... descriptors)
          throws Exception {return getInitializedTestHarness(
        function, maxParallelism, numTasks, taskIdx, null, descriptors);
  }public static <IN1, IN2, OUT>
      BroadcastStreamNonKeyedOperatorTestHarness<IN1, IN2, OUT> getInitializedTestHarness(
          final BroadcastProcessFunction<IN1, IN2, OUT> function,
          final int maxParallelism,
          final int numTasks,
          final int taskIdx,
          final OperatorSubtaskState initState,
          final MapStateDescriptor<?, ?>... descriptors)
          throws Exception {BroadcastStreamNonKeyedOperatorTestHarness<IN1, IN2, OUT> testHarness =
        new BroadcastStreamNonKeyedOperatorTestHarness<>(
            new CoBroadcastWithNonKeyedOperator<>(
                Preconditions.checkNotNull(function), Arrays.asList(descriptors)),
            maxParallelism,
            numTasks,
            taskIdx);
    testHarness.setup();
    testHarness.initializeState(initState);
    testHarness.open();return testHarness;
  }
}
