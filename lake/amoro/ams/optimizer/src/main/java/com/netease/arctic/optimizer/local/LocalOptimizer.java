package com.netease.arctic.optimizer.local;

import com.netease.arctic.optimizer.LocalOptimizerContainer;
import com.netease.arctic.optimizer.Optimizer;
import com.netease.arctic.optimizer.OptimizerConfig;
import org.kohsuke.args4j.CmdLineException;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class LocalOptimizer {
  public static void main(String[] args) throws CmdLineException {
    OptimizerConfig optimizerConfig = new OptimizerConfig(args);
    Optimizer optimizer = new Optimizer(optimizerConfig);
    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    String processId = runtimeMXBean.getName().split("@")[0];
    optimizer.getToucher().withRegisterProperty(LocalOptimizerContainer.JOB_ID_PROPERTY, processId);
    optimizer.startOptimizing();
  }
}
