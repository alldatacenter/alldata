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
package org.apache.drill.exec.resourcemgr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class which helps in parsing memory configuration string using the passed in pattern to get memory value in
 * bytes.
 */
public class MemoryConfigParser {

  /**
   * @param memoryConfig Memory configuration string
   * @param patternToUse Pattern which will be used to parse the memory configuration string
   * @return memory value in bytes
   */
  public static long parseMemoryConfigString(String memoryConfig, String patternToUse) {
    Pattern pattern = Pattern.compile(patternToUse);
    Matcher patternMatcher = pattern.matcher(memoryConfig);

    long memoryPerNodeInBytes = 0;
    if (patternMatcher.matches()) {
      memoryPerNodeInBytes = Long.parseLong(patternMatcher.group(1));

      // group 2 can be optional
      String group2 = patternMatcher.group(2);
      if (!group2.isEmpty()) {
        switch (group2.charAt(0)) {
          case 'G':
          case 'g':
            memoryPerNodeInBytes *= 1073741824L;
            break;
          case 'K':
          case 'k':
            memoryPerNodeInBytes *= 1024L;
            break;
          case 'M':
          case 'm':
            memoryPerNodeInBytes *= 1048576L;
            break;
          default:
            throw new IllegalArgumentException(String.format("Memory Configuration %s didn't matched any of the" +
              " supported suffixes. [Details: Supported: kKmMgG, Actual: %s", memoryConfig, group2));
        }
      }
    } else {
      throw new IllegalArgumentException(String.format("Memory Configuration %s didn't matched supported format. " +
        "Supported format is %s?", memoryConfig, patternToUse));
    }

    return memoryPerNodeInBytes;
  }
}
