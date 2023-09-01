/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is to wrap multi elements to be as union key.
 */
public class UnionKey {
  private static final String SPLIT_KEY = "_";

  public static String buildKey(Object... factors) {
    return StringUtils.join(factors, SPLIT_KEY);
  }

  public static boolean startsWith(String key, Object... factors) {
    if (key == null) {
      return false;
    }
    return key.startsWith(buildKey(factors));
  }

  public static boolean sameWith(String key, Object... factors) {
    if (key == null) {
      return false;
    }
    return key.equals(buildKey(factors));
  }
}
