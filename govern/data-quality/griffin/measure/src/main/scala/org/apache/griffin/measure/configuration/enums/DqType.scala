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
 * effective when dsl type is "griffin-dsl",
 * indicates the dq type of griffin pre-defined measurements
 * <li> - The match percentage of items between source and target
 *                         count(source items matched with the ones from target) / count(source)
 *                         e.g.: source [1, 2, 3, 4, 5], target: [1, 2, 3, 4]
 *                         metric will be: { "total": 5, "miss": 1, "matched": 4 } accuracy is 80%.</li>
 * <li> - The statistic data of data source
 *                          e.g.: max, min, average, group by count, ...</li>
 * <li> - The uniqueness of data source comparing with itself
 *                           count(unique items in source) / count(source)
 *                           e.g.: [1, 2, 3, 3] -> { "unique": 2, "total": 4, "dup-arr": [ "dup": 1, "num": 1 ] }
 *                           uniqueness indicates the items without any replica of data</li>
 * <li> - The distinctness of data source comparing with itself
 *                             count(distinct items in source) / count(source)
 *                             e.g.: [1, 2, 3, 3] -> { "dist": 3, "total": 4, "dup-arr": [ "dup": 1, "num": 1 ] }
 *                             distinctness indicates the valid information of data
 *                             comparing with uniqueness, distinctness is more meaningful</li>
 * <li> - The latency of data source with timestamp information
 *                           e.g.: (receive_time - send_time)
 *                           timeliness can get the statistic metric of latency, like average, max, min,
 *                            percentile-value,
 *                           even more, it can record the items with latency above threshold you configured</li>
 * <li> - The completeness of data source
 *                             the columns you measure is incomplete if it is null</li>
 */
object DqType extends GriffinEnum {

  type DqType = Value

  val Accuracy, Profiling, Uniqueness, Duplicate, Distinct, Timeliness, Completeness = Value

  override def withNameWithDefault(name: String): enums.DqType.Value = {
    val dqType = super.withNameWithDefault(name)
    dqType match {
      case Uniqueness | Duplicate => Uniqueness
      case _ => dqType
    }
  }
}
