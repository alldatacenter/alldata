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
 * the strategy to flatten metric
 *  <li> -  default flatten strategy
 *                                     metrics contains 1 row -> flatten metric json map
 *                                     metrics contains n > 1 rows -> flatten metric json array
 *                                     n = 0: { }
 *                                     n = 1: { "col1": "value1", "col2": "value2", ... }
 *                                     n > 1: { "arr-name": [ { "col1": "value1", "col2": "value2", ... }, ... ] }
 *                                     all rows
 *  </li>
 *  <li> - metrics contains n rows -> flatten metric json map
 *                                    n = 0: { }
 *                                    n >= 1: { "col1": "value1", "col2": "value2", ... }
 *                                    the first row only
 *  </li>
 *  <li> -   metrics contains n rows -> flatten metric json array
 *                                    n = 0: { "arr-name": [ ] }
 *                                    n >= 1: { "arr-name": [ { "col1": "value1", "col2": "value2", ... }, ... ] }
 *                                    all rows
 *  </li>
 *  <li> - metrics contains n rows -> flatten metric json wrapped map
 *                                n = 0: { "map-name": { } }
 *                                n >= 1: { "map-name": { "col1": "value1", "col2": "value2", ... } }
 *                                the first row only
 *  </li>
 */
object FlattenType extends GriffinEnum {
  type FlattenType = Value

  val DefaultFlattenType, EntriesFlattenType, ArrayFlattenType, MapFlattenType =
    Value

  val List, Array, Entries, Map, Default = Value

  override def withNameWithDefault(name: String): Value = {
    val flattenType = super.withNameWithDefault(name)
    flattenType match {
      case Array | List => ArrayFlattenType
      case Map => MapFlattenType
      case Entries => EntriesFlattenType
      case _ => DefaultFlattenType
    }
  }
}
