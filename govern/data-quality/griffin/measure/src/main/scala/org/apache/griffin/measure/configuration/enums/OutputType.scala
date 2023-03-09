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

/**
 * the strategy to output metric
 *  <li> - output the rule step result as metric</li>
 *  <li> - output the rule step result as records</li>
 *  <li> - output the rule step result to update data source cache</li>
 *  <li> - will not output the result </li>
 */
object OutputType extends GriffinEnum {
  type OutputType = Value

  val MetricOutputType, RecordOutputType, DscUpdateOutputType, UnknownOutputType = Value

  val Metric, Record, Records, DscUpdate = Value

  override def withNameWithDefault(name: String): Value = {
    val flattenType = super.withNameWithDefault(name)
    flattenType match {
      case Metric => MetricOutputType
      case Record | Records => RecordOutputType
      case DscUpdate => DscUpdateOutputType
      case _ => UnknownOutputType
    }
  }
}
