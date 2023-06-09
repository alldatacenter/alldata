/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.drill.test.BaseTest;
import org.junit.Test;

// Easier to test Guava patching if we can reference Guava classes directly for the tests
// CHECKSTYLE:OFF
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.io.Closeables;
//CHECKSTYLE:ON

/**
 * Test class for {@code GuavaPatcher}
 *
 */
public class TestGuavaPatcher extends BaseTest {

  @Test
  public void checkSuccessfulPatching() {
    // Catch-all test to see if Guava patching was successful
    assertTrue("Guava Patcher ran with errors, check error log messages",
        GuavaPatcher.isPatchingSuccessful());
  }

  @Test
  public void checkStopwatchEllapsedMillis() throws Exception {
    long[] currentTimeMillis = new long[] { 0L };
    final Ticker ticker = new Ticker() {
      @Override
      public long read() {
        return TimeUnit.MILLISECONDS.toNanos(currentTimeMillis[0]);
      }
    };
    final Stopwatch stopwatch = Stopwatch.createStarted(ticker);
    currentTimeMillis[0] = 12345L;
    stopwatch.stop();

    assertEquals(currentTimeMillis[0],
        stopwatch.elapsed(TimeUnit.MILLISECONDS));
    assertEquals(currentTimeMillis[0], (long) invokeMethod(Stopwatch.class,
        "elapsedMillis", new Class[] {}, stopwatch));
  }

  @Test
  public void checkCloseablesCloseQuietly() throws Exception {
    final Closeable alwaysThrows = () -> {
      throw new IOException("Always fail");
    };

    invokeMethod(Closeables.class, "closeQuietly",
        new Class[] { Closeable.class }, null, alwaysThrows);
  }

  // All the preconditions checks are method which were previously added for
  // compatibility with Apache Iceberg but are not necessary anymore because
  // Guava's version has been updated since.

  @Test
  public void checkPreconditionsCheckArgumentIntParam() throws Exception {
    invokeMethod(Preconditions.class, "checkArgument",
        new Class[] { boolean.class, String.class, int.class }, null, true,
        "Error Message %s", 1);
    try {
      invokeMethod(Preconditions.class, "checkArgument",
          new Class[] { boolean.class, String.class, int.class }, null, false,
          "Error Message %s", 1);
      fail();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertEquals(IllegalArgumentException.class, cause.getClass());
      assertEquals("Error Message 1", cause.getMessage());
    }
  }

  @Test
  public void checkPreconditionsCheckArgumentLongParam() throws Exception {
    invokeMethod(Preconditions.class, "checkArgument",
        new Class[] { boolean.class, String.class, long.class }, null, true,
        "Error Message %s", 2L);
    try {
      invokeMethod(Preconditions.class, "checkArgument",
          new Class[] { boolean.class, String.class, long.class }, null, false,
          "Error Message %s", 2L);
      fail();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertEquals(IllegalArgumentException.class, cause.getClass());
      assertEquals("Error Message 2", cause.getMessage());
    }
  }

  @Test
  public void checkPreconditionsCheckArgumentLongLongParam() throws Exception {
    invokeMethod(Preconditions.class, "checkArgument",
        new Class[] { boolean.class, String.class, long.class, long.class },
        null, true, "Error Message %s %s", 3L, 4L);
    try {
      invokeMethod(Preconditions.class, "checkArgument",
          new Class[] { boolean.class, String.class, long.class, long.class },
          null, false, "Error Message %s %s", 3L, 4L);
      fail();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertEquals(IllegalArgumentException.class, cause.getClass());
      assertEquals("Error Message 3 4", cause.getMessage());
    }
  }

  @Test
  public void checkPreconditionsCheckNotNullIntParam() throws Exception {
    invokeMethod(Preconditions.class, "checkNotNull",
        new Class[] { Object.class, String.class, int.class }, null, this,
        "Error Message %s", 5);
    try {
      invokeMethod(Preconditions.class, "checkNotNull",
          new Class[] { Object.class, String.class, int.class }, null, null,
          "Error Message %s", 5);
      fail();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertEquals(NullPointerException.class, cause.getClass());
      assertEquals("Error Message 5", cause.getMessage());
    }
  }

  @Test
  public void checkPreconditionsCheckStateIntParam() throws Exception {
    invokeMethod(Preconditions.class, "checkState",
        new Class[] { boolean.class, String.class, int.class }, null, true,
        "Error Message %s", 6);
    try {
      invokeMethod(Preconditions.class, "checkState",
          new Class[] { boolean.class, String.class, int.class }, null, false,
          "Error Message %s", 6);
      fail();
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      assertEquals(IllegalStateException.class, cause.getClass());
      assertEquals("Error Message 6", cause.getMessage());
    }
  }

  @Test
  public void checkPreconditionsCheckNotNullVarargs() throws Exception {
    checkPreconditionsCheckVarargMethod("checkNotNull", Object.class, this,
        null, NullPointerException.class);
  }

  @Test
  public void checkPreconditionsCheckArgumentVarargs() throws Exception {
    checkPreconditionsCheckVarargMethod("checkArgument", boolean.class, true,
        false, IllegalArgumentException.class);
  }

  @Test
  public void checkPreconditionsCheckStateVarargs() throws Exception {
    checkPreconditionsCheckVarargMethod("checkState", boolean.class, true,
        false, IllegalStateException.class);
  }

  private <T> void checkPreconditionsCheckVarargMethod(String methodName,
      Class<T> argClass, T goodValue, T badValue,
      Class<? extends Throwable> exceptionClass) throws Exception {
    for (int i = 1; i < 5; i++) {
      String[] templatePlaceholders = new String[i];
      Arrays.fill(templatePlaceholders, "%s");

      Object[] templateArguments = new Object[i];
      Arrays.setAll(templateArguments, Integer::valueOf);

      String template = "Error message: "
          + Stream.of(templatePlaceholders).collect(Collectors.joining(","));
      String message = "Error message: " + Stream.of(templateArguments)
          .map(Object::toString).collect(Collectors.joining(","));

      Class<?>[] parameterTypes = new Class[2 + i];
      parameterTypes[0] = argClass;
      parameterTypes[1] = String.class;
      Arrays.fill(parameterTypes, 2, parameterTypes.length, Object.class);

      Object[] parameters = new Object[2 + i];
      parameters[0] = goodValue;
      parameters[1] = template;
      System.arraycopy(templateArguments, 0, parameters, 2, i);

      // Check successful call
      invokeMethod(Preconditions.class, methodName, parameterTypes, null,
          parameters);

      try {
        parameters[0] = badValue;
        invokeMethod(Preconditions.class, methodName, parameterTypes, null,
            parameters);
        fail();
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        assertEquals(exceptionClass, cause.getClass());
        assertEquals(message, cause.getMessage());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T invokeMethod(Class<?> clazz, String methodName,
      Class<?>[] argumentTypes, Object object, Object... arguments)
      throws Exception {
    Method method = clazz.getMethod(methodName, argumentTypes);
    return (T) method.invoke(object, arguments);
  }

}
