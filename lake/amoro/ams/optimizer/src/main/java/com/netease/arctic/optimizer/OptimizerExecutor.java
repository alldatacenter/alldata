package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.OptimizingTask;
import com.netease.arctic.ams.api.OptimizingTaskResult;
import com.netease.arctic.optimizing.OptimizingExecutor;
import com.netease.arctic.optimizing.OptimizingExecutorFactory;
import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.optimizing.TableOptimizing;
import com.netease.arctic.utils.ExceptionUtil;
import com.netease.arctic.utils.SerializationUtil;
import org.apache.iceberg.common.DynConstructors;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class OptimizerExecutor extends AbstractOptimizerOperator {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizerExecutor.class);

  private final int threadId;

  public OptimizerExecutor(OptimizerConfig config, int threadId) {
    super(config);
    this.threadId = threadId;
  }

  public void start() {
    while (isStarted()) {
      try {
        OptimizingTask task = pollTask();
        if (task != null && ackTask(task)) {
          OptimizingTaskResult result = executeTask(task);
          completeTask(result);
        }
      } catch (Throwable t) {
        LOG.error("Optimizer executor[{}] got an unexpected error", threadId, t);
      }
    }
  }

  public int getThreadId() {
    return threadId;
  }

  private OptimizingTask pollTask() {
    OptimizingTask task = null;
    while (isStarted()) {
      try {
        task = callAuthenticatedAms((client, token) -> client.pollTask(token, threadId));
      } catch (TException exception) {
        LOG.error("Optimizer executor[{}] polled task failed", threadId, exception);
      }
      if (task != null) {
        LOG.info("Optimizer executor[{}] polled task[{}] from ams", threadId, task.getTaskId());
        break;
      } else {
        waitAShortTime();
      }
    }
    return task;
  }

  private boolean ackTask(OptimizingTask task) {
    try {
      callAuthenticatedAms((client, token) -> {
        client.ackTask(token, threadId, task.getTaskId());
        return null;
      });
      LOG.info("Optimizer executor[{}] acknowledged task[{}] to ams", threadId, task.getTaskId());
      return true;
    } catch (TException exception) {
      LOG.error("Optimizer executor[{}] acknowledged task[{}] failed", threadId, task.getTaskId(), exception);
      return false;
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private OptimizingTaskResult executeTask(OptimizingTask task) {
    try {
      OptimizingInputProperties parse = OptimizingInputProperties.parse(task.getProperties());
      String executorFactoryImpl = parse.getExecutorFactoryImpl();
      TableOptimizing.OptimizingInput input = SerializationUtil.simpleDeserialize(task.getTaskInput());
      DynConstructors.Ctor<OptimizingExecutorFactory> ctor = DynConstructors.builder(OptimizingExecutorFactory.class)
          .impl(executorFactoryImpl).buildChecked();
      OptimizingExecutorFactory factory = ctor.newInstance();

      if (getConfig().isExtendDiskStorage()) {
        parse.enableSpillMap();
      }
      parse.setMaxSizeInMemory(getConfig().getMemoryStorageSize());
      parse.setSpillMapPath(getConfig().getDiskStoragePath());
      factory.initialize(parse.getProperties());

      OptimizingExecutor executor = factory.createExecutor(input);
      TableOptimizing.OptimizingOutput output = executor.execute();
      ByteBuffer outputByteBuffer = SerializationUtil.simpleSerialize(output);
      OptimizingTaskResult result = new OptimizingTaskResult(task.getTaskId(), threadId);
      result.setTaskOutput(outputByteBuffer);
      result.setSummary(output.summary());
      LOG.info("Optimizer executor[{}] executed task[{}]", threadId, task.getTaskId());
      return result;
    } catch (Throwable t) {
      LOG.error("Optimizer executor[{}] executed task[{}] failed", threadId, task.getTaskId(), t);
      OptimizingTaskResult errorResult = new OptimizingTaskResult(task.getTaskId(), threadId);
      errorResult.setErrorMessage(ExceptionUtil.getErrorMessage(t, 4000));
      return errorResult;
    }
  }

  private void completeTask(OptimizingTaskResult optimizingTaskResult) {
    try {
      callAuthenticatedAms((client, token) -> {
        client.completeTask(token, optimizingTaskResult);
        return null;
      });
      LOG.info("Optimizer executor[{}] completed task[{}] to ams", threadId, optimizingTaskResult.getTaskId());
    } catch (TException exception) {
      LOG.error("Optimizer executor[{}] completed task[{}] failed", threadId,
          optimizingTaskResult.getTaskId(), exception);
    }
  }
}
