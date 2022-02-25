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

package org.apache.griffin.measure.step

import scala.util.Try

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.context.DQContext

trait DQStep extends Loggable {

  val name: String

  /**
   * @return execution success
   */
  def execute(context: DQContext): Try[Boolean]

  def getNames: Seq[String] = name :: Nil

}

object DQStepStatus extends Enumeration {
  val PENDING: DQStepStatus.Value = Value
  val RUNNING: DQStepStatus.Value = Value
  val COMPLETE: DQStepStatus.Value = Value
  val FAILED: DQStepStatus.Value = Value
}
