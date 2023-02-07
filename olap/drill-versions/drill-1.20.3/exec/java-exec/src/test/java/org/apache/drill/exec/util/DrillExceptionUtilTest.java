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
package org.apache.drill.exec.util;

import org.apache.drill.common.exceptions.DrillIOException;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ErrorHelper;
import org.apache.drill.common.util.DrillExceptionUtil;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DrillExceptionUtilTest extends BaseTest {
  private static final String ERROR_MESSAGE = "Exception Test";
  private static final String NESTED_ERROR_MESSAGE = "Nested Exception";

  @Test
  public void testUnwrapException() {
    Throwable rootThrowable = new DrillRuntimeException(ERROR_MESSAGE);
    UserBitShared.ExceptionWrapper exceptionWrapper = ErrorHelper.getWrapper(rootThrowable);
    Throwable t = DrillExceptionUtil.getThrowable(exceptionWrapper);
    assertEquals("Exception class should match", rootThrowable.getClass(), t.getClass());
    assertEquals("Exception message should match", rootThrowable.getMessage(), t.getMessage());
  }

  @Test
  public void testUnwrapNestedException() {
    Throwable nested = new DrillIOException(NESTED_ERROR_MESSAGE);
    Throwable rootThrowable = new Error(ERROR_MESSAGE, nested);
    UserBitShared.ExceptionWrapper exceptionWrapper = ErrorHelper.getWrapper(rootThrowable);
    Throwable t = DrillExceptionUtil.getThrowable(exceptionWrapper);
    assertEquals("Exception class should match", rootThrowable.getClass(), t.getClass());
    assertEquals("Exception message should match", rootThrowable.getMessage(), t.getMessage());
    assertEquals("Exception cause class should match", rootThrowable.getCause().getClass(), t.getCause().getClass());
    assertEquals("Exception cause message should match", rootThrowable.getCause().getMessage(), t.getCause().getMessage());
  }
}
