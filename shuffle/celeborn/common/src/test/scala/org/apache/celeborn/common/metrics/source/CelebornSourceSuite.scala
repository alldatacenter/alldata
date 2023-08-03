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

package org.apache.celeborn.common.metrics.source

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.common.CelebornConf

class CelebornSourceSuite extends CelebornFunSuite {

  test("test getMetrics with customized label") {
    val conf = new CelebornConf()
    createAbstractSourceAndCheck(conf, "")
  }

  def createAbstractSourceAndCheck(conf: CelebornConf, extraLabels: String): Unit = {
    val mockSource = new AbstractSource(conf, "mock") {
      override def sourceName: String = "mockSource"
    }
    val user1 = Map("user" -> "user1")
    val user2 = Map("user" -> "user2")
    val user3 = Map("user" -> "user3")
    mockSource.addGauge("Gauge1") { () => 1000 }
    mockSource.addGauge("Gauge2", user1) { () => 2000 }
    mockSource.addCounter("Counter1")
    mockSource.addCounter("Counter2", user2)
    // test operation with and without label
    mockSource.incCounter("Counter1", 3000)
    mockSource.incCounter("Counter2", 4000, user2)
    mockSource.addTimer("Timer1")
    mockSource.addTimer("Timer2", user3)
    // ditto
    mockSource.startTimer("Timer1", "key1")
    mockSource.startTimer("Timer2", "key2", user3)
    Thread.sleep(10)
    mockSource.stopTimer("Timer1", "key1")
    mockSource.stopTimer("Timer2", "key2", user3)

    val res = mockSource.getMetrics()
    var extraLabelsStr = extraLabels
    if (extraLabels.nonEmpty) {
      extraLabelsStr = extraLabels + ","
    }
    val exp1 = s"""metrics_Gauge1_Value{${extraLabelsStr}role="mock"} 1000"""
    val exp2 = s"""metrics_Gauge2_Value{${extraLabelsStr}role="mock",user="user1"} 2000"""
    val exp3 = s"""metrics_Counter1_Count{${extraLabelsStr}role="mock"} 3000"""
    val exp4 = s"""metrics_Counter2_Count{${extraLabelsStr}role="mock",user="user2"} 4000"""
    val exp5 = s"""metrics_Timer1_Count{${extraLabelsStr}role="mock"} 1"""
    val exp6 = s"""metrics_Timer2_Count{${extraLabelsStr}role="mock",user="user3"} 1"""

    assert(res.contains(exp1))
    assert(res.contains(exp2))
    assert(res.contains(exp3))
    assert(res.contains(exp4))
    assert(res.contains(exp5))
    assert(res.contains(exp6))
  }

  test("test getMetrics with customized label by conf") {
    val conf = new CelebornConf()
    // label's is normal
    conf.set(CelebornConf.METRICS_EXTRA_LABELS.key, "l1=v1,l2=v2,l3=v3")
    val extraLabels = """l1="v1",l2="v2",l3="v3""""
    createAbstractSourceAndCheck(conf, extraLabels)

    // labels' kv not correct
    assertThrows[IllegalArgumentException] {
      conf.set(CelebornConf.METRICS_EXTRA_LABELS.key, "l1=v1,l2=")
      val extraLabels2 = """l1="v1",l2="v2",l3="v3""""
      createAbstractSourceAndCheck(conf, extraLabels2)
    }

    // there are spaces in labels
    conf.set(CelebornConf.METRICS_EXTRA_LABELS.key, " l1 = v1, l2  =v2  ,l3 =v3  ")
    val extraLabels3 = """l1="v1",l2="v2",l3="v3""""
    createAbstractSourceAndCheck(conf, extraLabels3)

  }
}
