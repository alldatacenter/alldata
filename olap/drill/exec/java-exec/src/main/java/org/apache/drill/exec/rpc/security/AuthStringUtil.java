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
package org.apache.drill.exec.rpc.security;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class AuthStringUtil {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthStringUtil.class);

  // ignores case
  public static boolean listContains(final List<String> list, final String toCompare) {
    for (final String string : list) {
      if (string.equalsIgnoreCase(toCompare)) {
        return true;
      }
    }
    return false;
  }

  // converts list if strings to set of uppercase strings
  public static Set<String> asSet(final List<String> list) {
    if (list == null) {
      return Sets.newHashSet();
    }
    return Sets.newHashSet(Iterators.transform(list.iterator(),
        new Function<String, String>() {
          @Nullable
          @Override
          public String apply(@Nullable String input) {
            return input == null ? null : input.toUpperCase();
          }
        }));
  }

  // prevent instantiation
  private AuthStringUtil() {
  }
}
