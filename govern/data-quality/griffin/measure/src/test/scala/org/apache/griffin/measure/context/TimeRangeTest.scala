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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

class TimeRangeTest extends AnyFlatSpec with Matchers {

  "time range" should "be able to merge another time range" in {
    val tr1 = TimeRange(1, 10, Set(2, 5, 8))
    val tr2 = TimeRange(4, 15, Set(5, 6, 13, 7))
    tr1.merge(tr2) should be(TimeRange(1, 15, Set(2, 5, 6, 7, 8, 13)))
  }

  it should "get minimum timestamp in not-empty timestamp set" in {
    val tr = TimeRange(1, 10, Set(2, 5, 8))
    tr.minTmstOpt should be(Some(2))
  }

  it should "not get minimum timestamp in empty timestamp set" in {
    val tr = TimeRange(1, 10, Set[Long]())
    tr.minTmstOpt should be(None)
  }

}
