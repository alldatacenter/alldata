package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestOptimizerToucher extends OptimizerTestBase {

  @Test
  public void testRegisterOptimizer() throws InterruptedException {
    OptimizerConfig optimizerConfig = OptimizerTestHelpers.buildOptimizerConfig(TEST_AMS.getServerUrl());
    OptimizerToucher optimizerToucher = new OptimizerToucher(optimizerConfig);

    TestTokenChangeListener tokenChangeListener = new TestTokenChangeListener();
    optimizerToucher.withTokenChangeListener(tokenChangeListener);
    optimizerToucher.withRegisterProperty("test_k", "test_v");
    new Thread(optimizerToucher::start).start();
    tokenChangeListener.waitForTokenChange();
    Assert.assertEquals(1, tokenChangeListener.tokenList().size());
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getRegisteredOptimizers().size());
    validateRegisteredOptimizer(tokenChangeListener.tokenList().get(0), optimizerConfig,
        Collections.singletonMap("test_k", "test_v"));

    // clear all optimizer, toucher will register again
    TEST_AMS.getOptimizerHandler().getRegisteredOptimizers().clear();
    tokenChangeListener.waitForTokenChange();
    Assert.assertEquals(2, tokenChangeListener.tokenList().size());
    Assert.assertEquals(1, TEST_AMS.getOptimizerHandler().getRegisteredOptimizers().size());
    validateRegisteredOptimizer(tokenChangeListener.tokenList().get(1), optimizerConfig,
        Collections.singletonMap("test_k", "test_v"));

    optimizerToucher.stop();
  }

  private void validateRegisteredOptimizer(String token, OptimizerConfig registerConfig,
      Map<String, String> optimizerProperties) {
    Map<String, OptimizerRegisterInfo> registeredOptimizerMap =
        TEST_AMS.getOptimizerHandler().getRegisteredOptimizers();
    Assert.assertTrue(registeredOptimizerMap.containsKey(token));

    OptimizerRegisterInfo registerInfo = registeredOptimizerMap.get(token);
    Assert.assertEquals(registerConfig.getResourceId(), registerInfo.getResourceId());
    Assert.assertEquals(registerConfig.getGroupName(), registerInfo.getGroupName());
    Assert.assertEquals(registerConfig.getMemorySize(), registerInfo.getMemoryMb());
    Assert.assertEquals(registerConfig.getExecutionParallel(), registerInfo.getThreadCount());
    Assert.assertEquals(optimizerProperties, registerInfo.getProperties());
  }

  static class TestTokenChangeListener implements OptimizerToucher.TokenChangeListener {
    private final List<String> tokenList = Lists.newArrayList();
    private transient CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void tokenChange(String newToken) {
      tokenList.add(newToken);
      latch.countDown();
    }

    List<String> tokenList() {
      return tokenList;
    }

    void waitForTokenChange() throws InterruptedException {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Wait for token change timeout");
      }
      latch = new CountDownLatch(1);
    }
  }
}
