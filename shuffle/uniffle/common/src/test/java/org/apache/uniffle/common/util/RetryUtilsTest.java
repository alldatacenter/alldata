/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.exception.NotRetryException;
import org.apache.uniffle.common.exception.RssException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RetryUtilsTest {
  @Test
  public void testRetry() {
    AtomicInteger tryTimes = new AtomicInteger();
    AtomicInteger callbackTime = new AtomicInteger();
    int maxTryTime = 3;
    try {
      RetryUtils.retry(() -> {
        tryTimes.incrementAndGet();
        throw new RssException("");
      }, () -> {
        callbackTime.incrementAndGet();
      }, 10, maxTryTime, Sets.newHashSet(RssException.class));
    } catch (Throwable throwable) {
      // ignore
    }
    assertEquals(tryTimes.get(), maxTryTime);
    assertEquals(callbackTime.get(), maxTryTime - 1);

    tryTimes.set(0);
    try {
      RetryUtils.retry(() -> {
        tryTimes.incrementAndGet();
        throw new Exception("");
      }, 10, maxTryTime);
    } catch (Throwable throwable) {
      // ignore
    }
    assertEquals(tryTimes.get(), maxTryTime);

    tryTimes.set(0);
    int ret = 0;
    try {
      ret = RetryUtils.retry(() -> {
        tryTimes.incrementAndGet();
        return 1;
      }, 10, maxTryTime);
    } catch (Throwable throwable) {
      // ignore
    }
    assertEquals(ret, 1);
    assertEquals(tryTimes.get(), 1);

    tryTimes.set(0);
    try {
      RetryUtils.retry(() -> {
        tryTimes.incrementAndGet();
        throw new NotRetryException("");
      }, 10, maxTryTime);
    } catch (Throwable throwable) {
      // ignore
    }
    assertEquals(tryTimes.get(), 1);
  }
}
