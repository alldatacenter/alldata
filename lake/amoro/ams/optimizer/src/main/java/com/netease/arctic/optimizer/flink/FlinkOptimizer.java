package com.netease.arctic.optimizer.flink;

import com.netease.arctic.optimizer.Optimizer;
import com.netease.arctic.optimizer.OptimizerConfig;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkOptimizer {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkOptimizer.class);

  public static final String JOB_ID_PROPERTY = "flink-job-id";

  private static final String JOB_NAME = "arctic-flink-optimizer";

  public static void main(String[] args) throws CmdLineException {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(new Configuration());
    OptimizerConfig optimizerConfig = new OptimizerConfig(args);
    Optimizer optimizer = new Optimizer(optimizerConfig);
    env.addSource(new FlinkToucher(optimizer.getToucher()))
        .setParallelism(1)
        .broadcast()
        .transform(FlinkExecutor.class.getName(), Types.VOID, new FlinkExecutor(optimizer.getExecutors()))
        .setParallelism(optimizerConfig.getExecutionParallel())
        .addSink(new DiscardingSink<>())
        .name("Optimizer empty sink")
        .setParallelism(1);

    try {
      env.execute(JOB_NAME);
    } catch (Exception e) {
      LOG.error("Execute flink optimizer failed", e);
    }
  }
}
