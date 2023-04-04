/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.listener;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;

public class ArcticRunListener extends RunListener {
  private static final Logger LOG = LoggerFactory.getLogger(ArcticRunListener.class);
  private long startTime;
  private long singleTestStartTime;

  private final PriorityQueue<TestCase> testCaseQueue = new PriorityQueue<>();

  @Override
  public void testRunStarted(Description description) {
    startTime = System.currentTimeMillis();
    LOG.info(
        "{} Tests started! Number of Test case: {}",
        description == null ? "Unknown" : description.getClassName(),
        description == null ? 0 : description.testCount());
  }

  @Override
  public void testRunFinished(Result result) {
    long endTime = System.currentTimeMillis();
    LOG.info("Tests finished! Number of test case: {}", result.getRunCount());
    long elapsedSeconds = (endTime - startTime) / 1000;
    LOG.info("Elapsed time of tests execution: {} seconds", elapsedSeconds);
    int printNum = Math.min(testCaseQueue.size(), 50);
    LOG.info("Print the top cost test case method name:");
    for (int i = 0; i < printNum; i++) {
      TestCase testCase = testCaseQueue.poll();
      Assert.assertNotNull(testCase);
      LOG.info("NO-{}, cost: {}ms, methodName:{}", i + 1, testCase.cost, testCase.methodName);
    }
  }

  @Override
  public void testStarted(Description description) {
    singleTestStartTime = System.currentTimeMillis();
    LOG.info("{} test is starting...", description.getMethodName());
  }

  @Override
  public void testFinished(Description description) {
    long cost = System.currentTimeMillis() - singleTestStartTime;
    testCaseQueue.add(TestCase.of(cost, description.getMethodName()));
    LOG.info("{} test is finished, cost {}ms...\n", description.getMethodName(), cost);
  }

  @Override
  public void testFailure(Failure failure) {
    LOG.info("{} test FAILED!!!", failure.getDescription().getMethodName());
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    super.testIgnored(description);
    Ignore ignore = description.getAnnotation(Ignore.class);
    LOG.info("@Ignore test method '{}', ignored reason '{}'.", description.getMethodName(), ignore.value());
  }

  private static class TestCase implements Comparable<TestCase> {
    private final Long cost;
    private final String methodName;

    private TestCase(long cost, String methodName) {
      this.cost = cost;
      this.methodName = methodName;
    }

    public static TestCase of(long cost, String methodName) {
      return new TestCase(cost, methodName);
    }

    @Override
    public int compareTo(ArcticRunListener.TestCase that) {
      Assert.assertNotNull(that);
      return that.cost.compareTo(cost);
    }
  }
}
