package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.PropertyNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Optimizer {
  private static final Logger LOG = LoggerFactory.getLogger(Optimizer.class);

  private final OptimizerConfig config;
  private final OptimizerToucher toucher;
  private final OptimizerExecutor[] executors;

  public Optimizer(OptimizerConfig config) {
    this.config = config;
    this.toucher = new OptimizerToucher(config);
    this.executors = new OptimizerExecutor[config.getExecutionParallel()];
    IntStream.range(0, config.getExecutionParallel()).forEach(i -> executors[i] = new OptimizerExecutor(config, i));
    if (config.getResourceId() != null) {
      toucher.withRegisterProperty(PropertyNames.RESOURCE_ID, config.getResourceId());
    }
  }

  public void startOptimizing() {
    LOG.info("Starting optimizer with configuration:{}", config);
    Arrays.stream(executors).forEach(optimizerExecutor -> {
      new Thread(
          optimizerExecutor::start,
          String.format("Optimizer-executor-%d", optimizerExecutor.getThreadId())).start();
    });
    toucher.withTokenChangeListener(new SetTokenToExecutors()).start();
  }

  public void stopOptimizing() {
    toucher.stop();
    Arrays.stream(executors).forEach(OptimizerExecutor::stop);
  }

  public OptimizerToucher getToucher() {
    return toucher;
  }

  public OptimizerExecutor[] getExecutors() {
    return executors;
  }

  class SetTokenToExecutors implements OptimizerToucher.TokenChangeListener {

    @Override
    public void tokenChange(String newToken) {
      Arrays.stream(executors).forEach(optimizerExecutor -> optimizerExecutor.setToken(newToken));
    }
  }

}
