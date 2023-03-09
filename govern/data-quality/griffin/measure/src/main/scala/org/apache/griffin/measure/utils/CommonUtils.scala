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

package org.apache.griffin.measure.utils

import java.util.concurrent.TimeUnit

import org.apache.griffin.measure.Loggable

object CommonUtils extends Loggable {

  /**
   * Executes a given code block and logs the time taken for its execution.
   *
   * @param f Arbitrary code block
   * @param timeUnit required for time conversion to desired unit. Default: [[TimeUnit.SECONDS]]
   * @tparam T resultant type parameter
   * @return result of type T
   */
  def timeThis[T](f: => T, timeUnit: TimeUnit = TimeUnit.SECONDS): T = {
    val startNanos = System.nanoTime()
    val result = f
    val endNanos = System.nanoTime()

    griffinLogger.info(s"Time taken: ${timeUnit
      .convert(endNanos - startNanos, TimeUnit.NANOSECONDS)} ${timeUnit.name().toLowerCase}")

    result
  }

  /**
   *
   * @param t `Traversable`
   * @param op operation
   * @tparam T1 any type
   * @return instance of `T1`
   */
  def safeReduce[T1](t: Iterable[T1])(op: (T1, T1) => T1): T1 = {
    val head :: tail = t.toList
    tail.fold(head)(op)
  }

}
