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

/**
 * Provides utilities (such as retrieving hints) to add more context to UserExceptions.
 */
public class UserExceptionUtils {
  public static final String USER_DOES_NOT_EXIST =
      "Username is absent in connection URL or doesn't exist on Drillbit node." +
          " Please specify a username in connection URL which is present on Drillbit node.";

  private UserExceptionUtils() {
    //Restrict instantiation
  }

  private static String decorateHint(final String text) {
    return String.format("[Hint: %s]", text);
  }
  public static String getUserHint(final Throwable ex) {
    if (ex.getMessage().startsWith("Error getting user info for current user")) {
      //User does not exist hint
      return decorateHint(USER_DOES_NOT_EXIST);
    } else {
      //No hint can be provided
      return "";
    }
  }
}
