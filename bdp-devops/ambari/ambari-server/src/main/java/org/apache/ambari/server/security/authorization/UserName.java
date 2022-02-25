/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security.authorization;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

/**
 * Represents an Ambari user name
 */
public class UserName {
  private static final char[] FORBIDDEN_CHARS = {'<', '>', '&', '|', '\\', '`'};
  private final String userName;

  /**
   * Creates a UserName from the given string
   */
  public static UserName fromString(String userName) {
    return new UserName(validated(userName));
  }

  private static String validated(String userName) {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException("Username cannot be empty");
    }
    rejectIfContainsAnyOf(userName, FORBIDDEN_CHARS);
    return userName;
  }

  private static void rejectIfContainsAnyOf(String name, char[] forbiddenChars) {
    for (char each : forbiddenChars) {
      if (name.contains(Character.toString(each))) {
        throw new IllegalArgumentException("Invalid username: " + name + " Avoid characters " + Arrays.toString(forbiddenChars));
      }
    }
  }

  private UserName(String userName) {
    this.userName = userName;
  }

  @Override
  public String toString() {
    return userName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserName userName1 = (UserName) o;
    return userName.equals(userName1.userName);
  }

  @Override
  public int hashCode() {
    return userName.hashCode();
  }
}
