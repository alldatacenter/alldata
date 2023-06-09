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
<@pp.changeOutputFile name="org/apache/drill/exec/store/StatisticsRecordCollector.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store;

import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import java.io.IOException;
import java.util.Map;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

/**
 * Interface for collecting and obtaining statistics.
 */
public interface StatisticsRecordCollector {

  /**
   * Called before starting writing fields in a record.
   * @throws IOException
   */
  void startStatisticsRecord() throws IOException;

  /**
   * Called after adding all fields in a particular statistics record are added using
   * add{TypeHolder}(fieldId, TypeHolder) methods.
   * @throws IOException
   */
  void endStatisticsRecord() throws IOException;

  /**
   * Returns true if this {@link StatisticsRecordCollector} has non-empty statistics.
   *
   * @return {@ode true} if this {@link StatisticsRecordCollector} has non-empty statistics
   */
  boolean hasStatistics();

  /**
   * Returns {@link DrillStatsTable.TableStatistics} instance with statistics collected using this {@link StatisticsRecordCollector}.
   *
   * @return {@link DrillStatsTable.TableStatistics} instance
   */
  DrillStatsTable.TableStatistics getStatistics();

  <#list vv.types as type>
  <#list type.minor as minor>
  <#list vv.modes as mode>

  /**
   * Add the field value given in <code>valueHolder</code> at the given column number <code>fieldId</code>.
   */
  public FieldConverter getNew${mode.prefix}${minor.class}Converter(int fieldId, String fieldName, FieldReader reader);
  </#list>
  </#list>
  </#list>
}
