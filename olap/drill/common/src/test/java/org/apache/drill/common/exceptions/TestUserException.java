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
package org.apache.drill.common.exceptions;

import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test various use cases when creating user exceptions
 */
public class TestUserException extends BaseTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
      .getLogger("--ignore.as.this.is.for.testing.exceptions--");

  private Exception wrap(UserException uex, int numWraps) {
    Exception ex = uex;
    for (int i = 0; i < numWraps; i++) {
      ex = new Exception("wrap #" + (i+1), ex);
    }

    return ex;
  }

  // make sure system exceptions are created properly
  @Test
  public void testBuildSystemException() {
    String message = "This is an exception";
    UserException uex = UserException.systemError(new Exception(new RuntimeException(message))).build(logger);

    Assert.assertTrue(uex.getOriginalMessage().contains(message));
    Assert.assertTrue(uex.getOriginalMessage().contains("RuntimeException"));

    DrillPBError error = uex.getOrCreatePBError(true);

    Assert.assertEquals(ErrorType.SYSTEM, error.getErrorType());
  }

  @Test
  public void testBuildUserExceptionWithMessage() {
    String message = "Test message";

    UserException uex = UserException.dataWriteError().message(message).build(logger);
    DrillPBError error = uex.getOrCreatePBError(false);

    Assert.assertEquals(ErrorType.DATA_WRITE, error.getErrorType());
    Assert.assertEquals(message, uex.getOriginalMessage());
  }

  @Test
  public void testBuildUserExceptionWithCause() {
    String message = "Test message";

    UserException uex = UserException.dataWriteError(new RuntimeException(message)).build(logger);
    DrillPBError error = uex.getOrCreatePBError(false);

    // cause message should be used
    Assert.assertEquals(ErrorType.DATA_WRITE, error.getErrorType());
    Assert.assertEquals(message, uex.getOriginalMessage());
  }

  @Test
  public void testBuildUserExceptionWithCauseAndMessage() {
    String messageA = "Test message A";
    String messageB = "Test message B";

    UserException uex = UserException.dataWriteError(new RuntimeException(messageA)).message(messageB).build(logger);
    DrillPBError error = uex.getOrCreatePBError(false);

    // passed message should override the cause message
    Assert.assertEquals(ErrorType.DATA_WRITE, error.getErrorType());
    Assert.assertFalse(error.getMessage().contains(messageA)); // messageA should not be part of the context
    Assert.assertEquals(messageB, uex.getOriginalMessage());
  }

  @Test
  public void testBuildUserExceptionWithUserExceptionCauseAndMessage() {
    String messageA = "Test message A";
    String messageB = "Test message B";

    UserException original = UserException.connectionError().message(messageA).build(logger);
    UserException uex = UserException.dataWriteError(wrap(original, 5)).message(messageB).build(logger);

    //builder should return the unwrapped original user exception and not build a new one
    Assert.assertEquals(original, uex);

    DrillPBError error = uex.getOrCreatePBError(false);
    Assert.assertEquals(messageA, uex.getOriginalMessage());
    Assert.assertFalse(error.getMessage().contains(messageB)); // messageB should not be part of the context
  }

  @Test
  public void testBuildUserExceptionWithFormattedMessage() {
    String format = "This is test #%d";

    UserException uex = UserException.connectionError().message(format, 5).build(logger);
    DrillPBError error = uex.getOrCreatePBError(false);

    Assert.assertEquals(ErrorType.CONNECTION, error.getErrorType());
    Assert.assertEquals(String.format(format, 5), uex.getOriginalMessage());
  }

  // make sure wrapped user exceptions are retrieved properly when calling ErrorHelper.wrap()
  @Test
  public void testWrapUserException() {
    UserException uex = UserException.dataReadError().message("this is a data read exception").build(logger);

    Exception wrapped = wrap(uex, 3);
    Assert.assertEquals(uex, UserException.systemError(wrapped).build(logger));
  }

}
