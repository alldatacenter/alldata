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

import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.buffer.DrillBuf;
import io.netty.util.IllegalReferenceCountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BoundsCheckingTest extends BaseTest {
  private static final Logger logger = LoggerFactory.getLogger(BoundsCheckingTest.class);

  private static boolean old;

  private RootAllocator allocator;

  private static boolean setBoundsChecking(boolean enabled) {
    String oldValue = System.setProperty(BoundsChecking.ENABLE_UNSAFE_BOUNDS_CHECK_PROPERTY, String.valueOf(enabled));
    return Boolean.parseBoolean(oldValue);
  }

  @BeforeClass
  public static void setBoundsCheckingEnabled() {
    old = setBoundsChecking(true);
  }

  @AfterClass
  public static void restoreBoundsChecking() {
    setBoundsChecking(old);
  }

  @Before
  public void setupAllocator() {
    allocator = new RootAllocator(Integer.MAX_VALUE);
  }

  @After
  public void closeAllocator() {
    allocator.close();
  }

  @Test
  public void testLengthCheck() {
    assertTrue(BoundsChecking.BOUNDS_CHECKING_ENABLED);
    try {
      BoundsChecking.lengthCheck(null, 0, 0);
      fail("expecting NullPointerException");
    } catch (NullPointerException e) {
      logger.debug("", e);
    }

    try (DrillBuf buffer = allocator.buffer(1)) {
      try {
        BoundsChecking.lengthCheck(buffer, 0, -1);
        fail("expecting IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        logger.debug("", e);
      }
      BoundsChecking.lengthCheck(buffer, 0, 0);
      BoundsChecking.lengthCheck(buffer, 0, 1);
      try {
        BoundsChecking.lengthCheck(buffer, 0, 2);
        fail("expecting IndexOutOfBoundsException");
      } catch (IndexOutOfBoundsException e) {
        logger.debug("", e);
      }
      try {
        BoundsChecking.lengthCheck(buffer, 2, 0);
        fail("expecting IndexOutOfBoundsException");
      } catch (IndexOutOfBoundsException e) {
        logger.debug("", e);
      }
      try {
        BoundsChecking.lengthCheck(buffer, -1, 0);
        fail("expecting IndexOutOfBoundsException");
      } catch (IndexOutOfBoundsException e) {
        logger.debug("", e);
      }
    }

    DrillBuf buffer = allocator.buffer(1);
    buffer.release();
    try {
      BoundsChecking.lengthCheck(buffer, 0, 0);
      fail("expecting IllegalReferenceCountException");
    } catch (IllegalReferenceCountException e) {
      logger.debug("", e);
    }
  }
}
