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

package com.netease.arctic.ams.server.controller;

import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * base controller
 */
public class RestBaseController {
  public static final String ALL_VALUE = "-1";

  protected static void checkOffsetAndLimit(int offset, int limit) {
    Preconditions.checkArgument(offset >= 0, "offset[%s] must >= 0", offset);
    Preconditions.checkArgument(limit >= 0, "limit[%s] must >= 0", limit);
  }

  protected static boolean filterAll(String value) {
    return StringUtils.isBlank(value) || ALL_VALUE.equals(value);
  }

  protected static boolean filterAll(List<String> values) {
    return CollectionUtils.isEmpty(values) ||
            values.size() == 1 &&
                    (StringUtils.isBlank(values.get(0)) || ALL_VALUE.equals(values.get(0)));
  }
}
