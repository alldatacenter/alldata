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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Created 2020/3/17.
 */
public class RetryUtils {
  private static final Logger LOG =
      LoggerFactory.getLogger(RetryUtils.class);

  @SuppressWarnings("unchecked")
  public static <T> T retryWithLimit(final int tryLimit, int interval, final Callable<T> callable, Class<? extends Throwable>... types) {
    int attempt = 0;
    Throwable lastException = null;

    while (attempt < tryLimit) {
      try {
        LOG.debug("Retry : {} retry.", attempt + 1);

        return callable.call();
      } catch (Throwable t) {
        if (types != null && types.length > 0 && !matches(t.getClass(), types)) {
          throw new RuntimeException(t);
        }

        try {
          Thread.sleep(interval);
        } catch (InterruptedException e1) {
          throw new RuntimeException("Retry interrupted with exception", t);
        }

        lastException = t;
        attempt++;
        LOG.warn("Retry {} failed.", attempt, lastException);
      }
    }

    LOG.error("All {} retries failed.", tryLimit, lastException);
    throw new RuntimeException("Retry limit hit with exception!", lastException);
  }

  private static boolean matches(Class<? extends Throwable> thrown, Class<? extends Throwable>[] types) {
    if (thrown == null || types == null) {
      return false;
    }

    for (Class<? extends Throwable> type : types) {
      if (type.isAssignableFrom(thrown)) {
        return true;
      }
    }

    return false;
  }
}
