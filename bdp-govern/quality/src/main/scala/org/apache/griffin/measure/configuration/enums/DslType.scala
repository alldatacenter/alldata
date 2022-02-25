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

package org.apache.griffin.measure.configuration.enums

import org.apache.griffin.measure.configuration.enums

/**
 * dsl type indicates the language type of rule param
 * <li> - spark-sql: rule defined in "SPARK-SQL" directly</li>
 * <li> - df-ops|df-opr|: data frame operations rule, support some pre-defined data frame ops()</li>
 * <li> - griffin dsl rule, to define dq measurements easier</li>
 */
object DslType extends GriffinEnum {
  type DslType = Value

  val SparkSql, GriffinDsl = Value

  /**
   *
   * @param name Dsltype from config file
   * @return Enum value corresponding to string
   */
  def withNameWithDslType(name: String): Value =
    values
      .find(_.toString.toLowerCase == name.replace("-", "").toLowerCase())
      .getOrElse(GriffinDsl)

  override def withNameWithDefault(name: String): enums.DslType.Value = {
    withNameWithDslType(name)
  }
}
