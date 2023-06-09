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
package org.apache.drill.exec.coord.zk;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.parquet.Strings;

import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A convenience class used to expedite zookeeper paths manipulations.
 */
public final class PathUtils {

  /**
   * Returns a normalized, combined path out of the given path segments.
   *
   * @param parts  path segments to combine
   * @see #normalize(String)
   */
  public static String join(final String... parts) {
    String path = Arrays.stream(parts)
        .peek(part -> Preconditions.checkNotNull(part, "parts cannot contain null"))
        .filter(part -> !Strings.isNullOrEmpty(part))
        .collect(Collectors.joining("/"));
    return normalize(path);
  }

  /**
   * Normalizes the given path eliminating repeated forward slashes.
   *
   * @return  normalized path
   */
  public static String normalize(final String path) {
    if (Strings.isNullOrEmpty(Preconditions.checkNotNull(path))) {
      return path;
    }

    final StringBuilder builder = new StringBuilder();
    char last = path.charAt(0);
    builder.append(last);
    for (int i=1; i<path.length(); i++) {
      char cur = path.charAt(i);
      if (last == '/' && cur == last) {
        continue;
      }
      builder.append(cur);
      last = cur;
    }
    return builder.toString();
  }

  /**
   * Creates and returns path with the protocol at the beginning from specified {@code url}.
   *
   * @param url the source of path and protocol
   * @return string with protocol and path divided by colon
   */
  public static String getPathWithProtocol(URL url) {
    if (url.getProtocol() != null) {
      return url.getProtocol() + ":" + url.getPath();
    } else {
      return url.getPath();
    }
  }
}
