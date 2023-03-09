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

package org.apache.griffin.measure.step.read

import org.apache.spark.sql._

import org.apache.griffin.measure.context.DQContext
import org.apache.griffin.measure.utils.DataFrameUtil._

case class UnionReadStep(name: String, readSteps: Seq[ReadStep]) extends ReadStep {

  val config: Map[String, Any] = Map()
  val cache: Boolean = false

  def read(context: DQContext): Option[DataFrame] = {
    val dfOpts = readSteps.map { readStep =>
      readStep.read(context)
    }
    if (dfOpts.nonEmpty) {
      dfOpts.reduce((a, b) => unionDfOpts(a, b))
    } else None
  }

}
