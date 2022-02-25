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

package org.apache.griffin.measure.step.builder

/**
 * for griffin dsl rules, the constant columns might be used during calculation,
 */
object ConstantColumns {
  val tmst = "__tmst"
  val metric = "__metric"
  val record = "__record"
  val empty = "__empty"

  val beginTs = "__begin_ts"
  val endTs = "__end_ts"

  val distinct = "__distinct"

  val rowNumber = "__rn"

  val columns: List[String] =
    List[String](tmst, metric, record, empty, beginTs, endTs, distinct, rowNumber)
}
