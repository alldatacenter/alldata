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
package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.fn.impl.RegexpUtil.SqlPatternInfo;

public class SqlPatternFactory {
  public static SqlPatternMatcher getSqlPatternMatcher(SqlPatternInfo patternInfo)
  {
    switch (patternInfo.getPatternType()) {
      case COMPLEX:
        return new SqlPatternComplexMatcher(patternInfo.getJavaPatternString());
      case STARTS_WITH:
        return new SqlPatternStartsWithMatcher(patternInfo.getSimplePatternString());
      case CONSTANT:
        return new SqlPatternConstantMatcher(patternInfo.getSimplePatternString());
      case ENDS_WITH:
        return new SqlPatternEndsWithMatcher(patternInfo.getSimplePatternString());
      case CONTAINS:
        return new SqlPatternContainsMatcher(patternInfo.getSimplePatternString());
      default:
        break;
    }

    return null;
  }
}
