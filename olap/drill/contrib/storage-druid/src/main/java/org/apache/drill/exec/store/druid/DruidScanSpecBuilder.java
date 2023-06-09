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
package org.apache.drill.exec.store.druid;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.store.druid.common.DruidBoundFilter;
import org.apache.drill.exec.store.druid.common.DruidConstants;
import org.apache.drill.exec.store.druid.common.DruidFilter;
import org.apache.drill.exec.store.druid.common.DruidIntervalFilter;
import org.apache.drill.exec.store.druid.common.DruidNotFilter;
import org.apache.drill.exec.store.druid.common.DruidRegexFilter;
import org.apache.drill.exec.store.druid.common.DruidSearchFilter;
import org.apache.drill.exec.store.druid.common.DruidSearchQuerySpec;
import org.apache.drill.exec.store.druid.common.DruidSelectorFilter;

public class DruidScanSpecBuilder {

  private static final String REGEX_KEYWORD_HINT = "$regex$_";

  DruidScanSpec build(String dataSourceName,
                      long dataSourceSize,
                      String maxTime,
                      String minTime,
                      String functionName,
                      SchemaPath field,
                      Object fieldValue) {
    String fieldName = field.getAsNamePart().getName();
    DruidFilter filter = translateFilter(functionName, fieldName, String.valueOf(fieldValue));
    return (filter == null) ? null : new DruidScanSpec(dataSourceName, filter, dataSourceSize, maxTime, minTime);
  }

  private DruidFilter translateFilter(String functionName, String fieldName, String fieldValue) {
    switch (functionName) {
      case FunctionNames.EQ: {
        if (fieldName.equalsIgnoreCase(DruidConstants.INTERVAL_DIMENSION_NAME)) {
          return new DruidIntervalFilter(fieldValue);
        } else {
          return new DruidSelectorFilter(fieldName, fieldValue);
        }
      }
      case FunctionNames.NE: {
        DruidSelectorFilter druidSelectorFilter = new DruidSelectorFilter(fieldName, fieldValue);
        return new DruidNotFilter(druidSelectorFilter);
      }
      case FunctionNames.GE: {
        return new DruidBoundFilter(fieldName, fieldValue, null, null, null);
      }
      case FunctionNames.GT: {
        return new DruidBoundFilter(fieldName, fieldValue, null, true, null);
      }
      case FunctionNames.LE: {
        return new DruidBoundFilter(fieldName, null, fieldValue, null, null);
      }
      case FunctionNames.LT: {
        return new DruidBoundFilter(fieldName, null, fieldValue, null, true);
      }
      case FunctionNames.IS_NULL: {
        return new DruidSelectorFilter(fieldName, null);
      }
      case FunctionNames.IS_NOT_NULL: {
        DruidSelectorFilter druidSelectorFilter = new DruidSelectorFilter(fieldName, null);
        return new DruidNotFilter(druidSelectorFilter);
      }
      case FunctionNames.LIKE: {
        if (fieldValue.startsWith(REGEX_KEYWORD_HINT) ) {
          return new DruidRegexFilter(fieldName, fieldValue.substring(REGEX_KEYWORD_HINT.length()));
        } else {
          String fieldValueNormalized = StringUtils.removeStart(StringUtils.removeEnd(fieldValue, "%"), "%");
          return new DruidSearchFilter(fieldName, new DruidSearchQuerySpec(fieldValueNormalized, false));
        }
      }
      default:
        return null;
    }
  }
}
