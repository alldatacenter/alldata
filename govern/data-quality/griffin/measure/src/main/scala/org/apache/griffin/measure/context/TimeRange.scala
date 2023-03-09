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

package org.apache.griffin.measure.context

import scala.math.{max, min}

case class TimeRange(begin: Long, end: Long, tmsts: Set[Long]) extends Serializable {
  def merge(tr: TimeRange): TimeRange = {
    TimeRange(min(begin, tr.begin), max(end, tr.end), tmsts ++ tr.tmsts)
  }
  def minTmstOpt: Option[Long] = {
    try {
      if (tmsts.nonEmpty) Some(tmsts.min) else None
    } catch {
      case _: Throwable => None
    }
  }
}

object TimeRange {
  val emptyTimeRange: TimeRange = TimeRange(0, 0, Set[Long]())
  def apply(range: (Long, Long), tmsts: Set[Long]): TimeRange =
    TimeRange(range._1, range._2, tmsts)
  def apply(ts: Long, tmsts: Set[Long]): TimeRange = TimeRange(ts, ts, tmsts)
  def apply(ts: Long): TimeRange = TimeRange(ts, ts, Set[Long](ts))
  def apply(tmsts: Set[Long]): TimeRange = {
    try {
      TimeRange(tmsts.min, tmsts.max, tmsts)
    } catch {
      case _: Throwable => emptyTimeRange
    }
  }
}
