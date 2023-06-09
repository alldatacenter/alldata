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
<@pp.dropOutputFile />
<#assign className="GAge"/>

<@pp.changeOutputFile name="/org/apache/drill/exec/expr/fn/impl/${className}.java"/>

<#include "/@includes/license.ftl"/>

package org.apache.drill.exec.expr.fn.impl;

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import javax.inject.Inject;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.*;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.ops.ContextInformation;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

public class ${className} {

<#list dateIntervalFunc.dates as fromUnit>
<#list dateIntervalFunc.dates as toUnit>

  /**
   * Binary form, returns the interval between `right` and `left`.
   * Note that this function does not count calendar boundary crossings,
   * e.g. between yesterday 23:00 and today 01:00 is two hours, not one day.
   * Modeled on the AGE function in PostgreSQL, see
   * https://www.postgresql.org/docs/current/functions-datetime.html.
   */
  @FunctionTemplate(name = "age",
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class Age${fromUnit}To${toUnit} implements DrillSimpleFunc {

    @Param ${toUnit}Holder left;
    @Param ${fromUnit}Holder right;
    @Output IntervalHolder out;

    public void setup() {
    }

    public void eval() {
      java.time.LocalDateTime from = java.time.Instant.ofEpochMilli(right.value).atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
      java.time.LocalDateTime to = java.time.Instant.ofEpochMilli(left.value).atZone(java.time.ZoneOffset.UTC).toLocalDateTime();

      long months = from.until(to, java.time.temporal.ChronoUnit.MONTHS);
      from = from.plusMonths(months);
      long days = from.until(to, java.time.temporal.ChronoUnit.DAYS);
      from = from.plusDays(days);
      long millis = from.until(to, java.time.temporal.ChronoUnit.MILLIS);

      out.months = (int) months;
      out.days = (int) days;
      out.milliseconds = (int) millis;
    }
  }
</#list>
  /**
   * Unary form, subtracts `right` from midnight so equivalent to
   * `select age(current_date, right)`.
   * Note that this function does not count calendar boundary crossings,
   * e.g. between yesterday 23:00 and today 01:00 is two hours, not one day.
   * Modeled on the AGE function in PostgreSQL, see
   * https://www.postgresql.org/docs/current/functions-datetime.html.
   */
  @FunctionTemplate(name = "age",
                    scope = FunctionTemplate.FunctionScope.SIMPLE,
                    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class Age${fromUnit}ToMidnight implements DrillSimpleFunc {

    @Param ${fromUnit}Holder right;
    @Workspace java.time.LocalDateTime to;
    @Output IntervalHolder out;
    @Inject ContextInformation contextInfo;

    public void setup() {
      java.time.ZoneId zoneId = contextInfo.getRootFragmentTimeZone();
      java.time.ZonedDateTime zdtStart = contextInfo.getQueryStartInstant().atZone(zoneId);
      to = zdtStart.truncatedTo(java.time.temporal.ChronoUnit.DAYS).toLocalDateTime();
    }

    public void eval() {
      java.time.LocalDateTime from = java.time.Instant.ofEpochMilli(right.value).atZone(java.time.ZoneOffset.UTC).toLocalDateTime();

      long months = from.until(to, java.time.temporal.ChronoUnit.MONTHS);
      from = from.plusMonths(months);
      long days = from.until(to, java.time.temporal.ChronoUnit.DAYS);
      from = from.plusDays(days);
      long millis = from.until(to, java.time.temporal.ChronoUnit.MILLIS);

      out.months = (int) months;
      out.days = (int) days;
      out.milliseconds = (int) millis;
    }
  }
</#list>
}
