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
package org.apache.drill.exec.memory;

import java.lang.reflect.Field;
import java.util.Formatter;

import org.apache.drill.exec.util.AssertionUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.DrillBuf;
import io.netty.util.IllegalReferenceCountException;

import static org.apache.drill.exec.util.SystemPropertyUtil.getBoolean;

public class BoundsChecking {
  private static final Logger logger = LoggerFactory.getLogger(BoundsChecking.class);

  public static final String ENABLE_UNSAFE_BOUNDS_CHECK_PROPERTY = "drill.exec.memory.enable_unsafe_bounds_check";
  public static final String ENABLE_UNSAFE_MEMORY_ACCESS_PROPERTY = "drill.enable_unsafe_memory_access";
  public static final boolean BOUNDS_CHECKING_ENABLED = boundsCheckEnabled();
  private static final boolean checkAccessible = getStaticBooleanField(AbstractByteBuf.class, "checkAccessible", false);

  /**
   * Bounds checking is on either if it is explicitly turned on via a
   * supported option, or if we're running with assertions enabled, typically
   * in an IDE.
   */
  private static boolean boundsCheckEnabled() {
    // for backward compatibility check "drill.enable_unsafe_memory_access" property and enable bounds checking when
    // unsafe memory access is explicitly disabled.
    // Enable bounds checking if assertions are enabled (as they are in IDE runs.)
    return AssertionUtil.isAssertionsEnabled() ||
        getBoolean(ENABLE_UNSAFE_BOUNDS_CHECK_PROPERTY,
            !getBoolean(ENABLE_UNSAFE_MEMORY_ACCESS_PROPERTY, true));
  }

  static {
    if (BOUNDS_CHECKING_ENABLED) {
      logger.warn("Drill is running with direct memory bounds checking enabled. If this is a production system, disable it.");
    } else if (logger.isDebugEnabled()) {
      logger.debug("Direct memory bounds checking is disabled.");
    }
  }

  private BoundsChecking() { }

  private static boolean getStaticBooleanField(Class<?> cls, String name, boolean def) {
    try {
      Field field = cls.getDeclaredField(name);
      field.setAccessible(true);
      return field.getBoolean(null);
    } catch (ReflectiveOperationException e) {
      return def;
    }
  }

  private static void checkIndex(DrillBuf buf, int index, int fieldLength) {
    Preconditions.checkNotNull(buf);
    if (checkAccessible && buf.refCnt() == 0) {
      Formatter formatter = new Formatter().format("%s, refCnt: 0", buf);
      if (BaseAllocator.DEBUG) {
        formatter.format("%n%s", buf.toVerboseString());
      }
      throw new IllegalReferenceCountException(formatter.toString());
    }
    if (fieldLength < 0) {
      throw new IllegalArgumentException(String.format("length: %d (expected: >= 0)", fieldLength));
    }
    if (index < 0 || index > buf.capacity() - fieldLength) {
      Formatter formatter = new Formatter().format("%s, index: %d, length: %d (expected: range(0, %d))", buf, index, fieldLength, buf.capacity());
      if (BaseAllocator.DEBUG) {
        formatter.format("%n%s", buf.toVerboseString());
      }
      throw new IndexOutOfBoundsException(formatter.toString());
    }
  }

  public static void lengthCheck(DrillBuf buf, int start, int length) {
    if (BOUNDS_CHECKING_ENABLED) {
      checkIndex(buf, start, length);
    }
  }

  public static void rangeCheck(DrillBuf buf, int start, int end) {
    if (BOUNDS_CHECKING_ENABLED) {
      checkIndex(buf, start, end - start);
    }
  }

  public static void rangeCheck(DrillBuf buf1, int start1, int end1, DrillBuf buf2, int start2, int end2) {
    if (BOUNDS_CHECKING_ENABLED) {
      checkIndex(buf1, start1, end1 - start1);
      checkIndex(buf2, start2, end2 - start2);
    }
  }

  public static void ensureWritable(DrillBuf buf, int minWritableBytes) {
    if (BOUNDS_CHECKING_ENABLED) {
      buf.ensureWritable(minWritableBytes);
    }
  }
}
