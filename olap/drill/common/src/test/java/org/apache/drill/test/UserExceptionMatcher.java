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
package org.apache.drill.test;


import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for UserException that matches if expected type and actual type are the same, and expected message is
 * contained in the actual message.
 */
public class UserExceptionMatcher extends TypeSafeMatcher<UserException> {

  private final ErrorType expectedType;
  private final String expectedMessage;

  public UserExceptionMatcher(final ErrorType expectedType, final String expectedMessage) {
    this.expectedType = expectedType;
    this.expectedMessage = expectedMessage;
  }

  public UserExceptionMatcher(final ErrorType expectedType) {
    this(expectedType, null);
  }

  @Override
  protected boolean matchesSafely(final UserException e) {
    // Use .contains(...) to compare expected and actual message as the exact messages may differ.
    return expectedType == e.getErrorType() && (expectedMessage == null || e.getMessage().contains(expectedMessage));
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("UserException of type: ")
      .appendValue(expectedType.toString());
    if (expectedMessage != null) {
      description.appendText(" with message that contains: \"")
        .appendText(expectedMessage)
        .appendText("\"");
    }
  }

  @Override
  protected void describeMismatchSafely(final UserException e, final Description description) {
    description.appendText("UserException thrown was of type: ")
      .appendValue(e.getErrorType().toString());
    if (expectedMessage != null) {
      description.appendText(" with message: \"")
        .appendText(e.getMessage())
      .appendText("\"");
    }
  }
}
