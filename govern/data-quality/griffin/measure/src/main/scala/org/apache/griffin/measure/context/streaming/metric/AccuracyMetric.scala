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

package org.apache.griffin.measure.context.streaming.metric

/**
 * accuracy metric
 * @param miss     miss count
 * @param total    total count
 */
case class AccuracyMetric(miss: Long, total: Long) extends Metric {

  type T = AccuracyMetric

  override def isLegal: Boolean = getTotal > 0

  def update(delta: T): T = {
    if (delta.miss < miss) AccuracyMetric(delta.miss, total) else this
  }

  def initial(): Boolean = {
    getMatch <= 0
  }

  def eventual(): Boolean = {
    this.miss <= 0
  }

  def differsFrom(other: T): Boolean = {
    (this.miss != other.miss) || (this.total != other.total)
  }

  def getMiss: Long = miss

  def getTotal: Long = total

  def getMatch: Long = total - miss

  def matchFraction: Double = if (getTotal <= 0) 1 else getMatch.toDouble / getTotal

  def matchPercentage: Double = matchFraction * 100

}
