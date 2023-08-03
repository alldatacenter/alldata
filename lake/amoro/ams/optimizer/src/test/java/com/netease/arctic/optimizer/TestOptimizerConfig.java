package com.netease.arctic.optimizer;

import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;

import java.util.UUID;

public class TestOptimizerConfig {

  @Test
  public void testParseArguments() throws CmdLineException {
    String cmd = "-a thrift://127.0.0.1:1260 -p 11 -m 1024 -g g1 -hb 2000 -eds -dsp /tmp/arctic -msz 512";
    String[] args = cmd.split(" ");
    OptimizerConfig optimizerConfig = new OptimizerConfig(args);
    Assert.assertEquals("thrift://127.0.0.1:1260", optimizerConfig.getAmsUrl());
    Assert.assertEquals(11, optimizerConfig.getExecutionParallel());
    Assert.assertEquals(1024, optimizerConfig.getMemorySize());
    Assert.assertEquals("g1", optimizerConfig.getGroupName());
    Assert.assertEquals(2000, optimizerConfig.getHeartBeat());
    Assert.assertTrue(optimizerConfig.isExtendDiskStorage());
    Assert.assertEquals("/tmp/arctic", optimizerConfig.getDiskStoragePath());
    Assert.assertEquals(512, optimizerConfig.getMemoryStorageSize());
  }

  @Test
  public void testSetAndGet() {
    OptimizerConfig config = new OptimizerConfig();
    String amsUrl = "thrift://127.0.0.1:1260";
    int executionParallel = 4;
    int memorySize = 1024;
    String groupName = "testGroup";
    long heartBeat = 20000;
    String diskStoragePath = "/tmp";
    long memoryStorageSize = 1024;
    String resourceId = UUID.randomUUID().toString();

    config.setAmsUrl(amsUrl);
    config.setExecutionParallel(executionParallel);
    config.setMemorySize(memorySize);
    config.setGroupName(groupName);
    config.setHeartBeat(heartBeat);
    config.setExtendDiskStorage(true);
    config.setDiskStoragePath(diskStoragePath);
    config.setMemoryStorageSize(memoryStorageSize);
    config.setResourceId(resourceId);

    Assert.assertEquals(amsUrl, config.getAmsUrl());
    Assert.assertEquals(executionParallel, config.getExecutionParallel());
    Assert.assertEquals(memorySize, config.getMemorySize());
    Assert.assertEquals(groupName, config.getGroupName());
    Assert.assertEquals(heartBeat, config.getHeartBeat());
    Assert.assertTrue(config.isExtendDiskStorage());
    Assert.assertEquals(diskStoragePath, config.getDiskStoragePath());
    Assert.assertEquals(memoryStorageSize, config.getMemoryStorageSize());
    Assert.assertEquals(resourceId, config.getResourceId());
  }

  @Test(expected = CmdLineException.class)
  public void testMissingRequiredArgs() throws CmdLineException {
    String[] args = {"-a", "thrift://127.0.0.1:1260", "-p", "4", "-g", "testGroup"};
    new OptimizerConfig(args);
  }

  @Test(expected = CmdLineException.class)
  public void testInvalidArgs() throws CmdLineException {
    String[] args = {"-a", "thrift://127.0.0.1:1260", "-p", "invalid", "-m", "1024", "-g", "testGroup"};
    new OptimizerConfig(args);
  }

  @Test(expected = CmdLineException.class)
  public void testMissingValueArgs() throws CmdLineException {
    String[] args = {"-a", "thrift://127.0.0.1:1260", "-p", "-m", "1024", "-g", "testGroup"};
    new OptimizerConfig(args);
  }
}
