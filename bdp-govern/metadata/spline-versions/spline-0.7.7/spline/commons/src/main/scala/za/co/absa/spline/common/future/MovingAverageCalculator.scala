/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.common.future

import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongBinaryOperator

class MovingAverageCalculator(initialValue: Long, alpha: Double) {

  private val lastAvg = new AtomicLong(initialValue)

  def currentAverage: Long = lastAvg.get

  def addMeasurement(elapsedTime: Long): Unit =
    lastAvg.accumulateAndGet(elapsedTime, new LongBinaryOperator {
      override def applyAsLong(left: Long, right: Long): Long =
        (alpha * elapsedTime + (1 - alpha) * lastAvg.get).toLong
    })
}
