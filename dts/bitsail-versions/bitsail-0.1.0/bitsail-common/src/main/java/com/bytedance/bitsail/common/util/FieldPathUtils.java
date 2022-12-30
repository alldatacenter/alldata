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

package com.bytedance.bitsail.common.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created 2020/5/29.
 */
public class FieldPathUtils {
  private static final Pattern LIST_VALUE_PATTERN = Pattern.compile("^(.*)\\[([0-9].*)\\]$");

  public static PathInfo getPathInfo(String column) {
    String[] splitColumnNames = StringUtils.split(column.trim(), ".", 2);
    if (ArrayUtils.getLength(splitColumnNames) == 1) {
      return parseSinglePathInfo(splitColumnNames[0]);
    }
    PathInfo root = parseSinglePathInfo(splitColumnNames[0]);
    root.setNestPathInfo(getPathInfo(splitColumnNames[1]));
    return root;
  }

  private static PathInfo parseSinglePathInfo(String column) {
    Matcher matcher = listMatcher(column);
    PathInfo.PathInfoBuilder builder = PathInfo.builder();
    if (matcher.matches()) {
      builder.name(matcher.group(1))
          .pathType(PathType.ARRAY)
          .index(Integer.parseInt(matcher.group(2)));
    } else {
      builder.name(column)
          .pathType(PathType.OTHER);
    }
    return builder.build();
  }

  public static Matcher listMatcher(String column) {
    return LIST_VALUE_PATTERN.matcher(column);
  }

  public static enum PathType {

    /**
     * Array list
     */
    ARRAY,

    /**
     *
     */
    OTHER;

  }

  @Builder
  @AllArgsConstructor
  @Data
  public static class PathInfo implements Serializable {
    private static final long serialVersionUID = -9037137551136429645L;

    private String name;

    private int index;

    private PathType pathType;

    private PathInfo nestPathInfo;
  }
}
