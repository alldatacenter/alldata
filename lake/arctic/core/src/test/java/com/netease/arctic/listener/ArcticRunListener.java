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

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcticRunListener extends RunListener {
  private static final Logger LOG = LoggerFactory.getLogger(ArcticRunListener.class);
  private long startTime;

  @Override
  public void testRunStarted(Description description) {
    startTime = System.currentTimeMillis();
    LOG.info(
        "{} Tests started! Number of Test case: {}",
        description == null ? "Unknown" : description.getClassName(),
        description == null ? 0 : description.testCount());
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    long endTime = System.currentTimeMillis();
    LOG.info("Tests finished! Number of test case: {}", result.getRunCount());
    long elapsedSeconds = (endTime - startTime) / 1000;
    LOG.info("Elapsed time of tests execution: " + elapsedSeconds + " seconds");
  }

  @Override
  public void testStarted(Description description) {
    LOG.info(description.getMethodName() + " test is starting...");
  }

  @Override
  public void testFinished(Description description) {
    LOG.info(description.getMethodName() + " test is finished...\n");
  }

  @Override
  public void testFailure(Failure failure) {
    LOG.info(failure.getDescription().getMethodName() + " test FAILED!!!");
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    super.testIgnored(description);
    Ignore ignore = description.getAnnotation(Ignore.class);
    LOG.info("@Ignore test method '{}': '%{}'", description.getMethodName(), ignore.value());
  }
}
