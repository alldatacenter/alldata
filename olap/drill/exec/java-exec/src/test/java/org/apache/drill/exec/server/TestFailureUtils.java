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
package org.apache.drill.exec.server;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.apache.drill.exec.server.FailureUtils.DIRECT_MEMORY_OOM_MESSAGE;

public class TestFailureUtils extends BaseTest {
  @Test
  public void testIsDirectMemoryOOM() {
    Assert.assertTrue(FailureUtils.isDirectMemoryOOM(new OutOfMemoryException()));
    Assert.assertFalse(FailureUtils.isDirectMemoryOOM(new OutOfMemoryError()));
    Assert.assertFalse(FailureUtils.isDirectMemoryOOM(new OutOfMemoryError("Heap went boom.")));
    Assert.assertTrue(FailureUtils.isDirectMemoryOOM(new OutOfMemoryError(DIRECT_MEMORY_OOM_MESSAGE)));
  }

  @Test
  public void testIsHeapOOM() {
    Assert.assertTrue(FailureUtils.isHeapOOM(new OutOfMemoryError()));
    Assert.assertTrue(FailureUtils.isHeapOOM(new OutOfMemoryError("Heap went boom.")));
    Assert.assertFalse(FailureUtils.isHeapOOM(new OutOfMemoryError(DIRECT_MEMORY_OOM_MESSAGE)));
    Assert.assertFalse(FailureUtils.isHeapOOM(new IOException()));
    Assert.assertFalse(FailureUtils.isHeapOOM(new NullPointerException()));
    Assert.assertFalse(FailureUtils.isHeapOOM(new OutOfMemoryException()));
  }
}
