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

import static org.junit.Assert.fail;

public class DrillAssert {

  public static void assertMultiLineStringEquals(String expected, String actual) {
    assertMultiLineStringEquals(null, expected, actual);
  }

  public static void assertMultiLineStringEquals(String message, String expected, String actual) {
    outside:
    if (expected == actual) {
      return;
    } else if (expected != null && actual != null) {
      int idx1 = 0, idx2 = 0;
      char ch1, ch2;
      while (idx1 < expected.length() && idx2 < actual.length()) {
        ch1 = expected.charAt(idx1);
        ch2 = actual.charAt(idx2);
        if (isNewLineChar(ch1)) {
          idx1++;
          continue;
        } else if (isNewLineChar(ch2)) {
          idx2++;
          continue;
        } else if (ch1 != ch2) {
          break outside;
        } else {
          idx1++;
          idx2++;
        }
      }
      // skip newlines at the end
      while(idx1 < expected.length() && isNewLineChar(expected.charAt(idx1))) {
        idx1++;
      }
      while(idx2 < actual.length() && isNewLineChar(actual.charAt(idx2))) {
        idx2++;
      }
      if (idx1 == expected.length() && idx2 == actual.length()) {
        return;
      }
    }

    fail(message != null ? message : "Expected: " + expected + ", but was: " + actual);
  }

  private static boolean isNewLineChar(char ch) {
    return (ch == '\r' || ch == '\n');
  }

}
