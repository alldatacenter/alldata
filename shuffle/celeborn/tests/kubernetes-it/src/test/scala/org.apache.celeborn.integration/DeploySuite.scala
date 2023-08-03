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

package org.apache.celeborn.integration

import scala.language.postfixOps

import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.{interval, timeout}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import org.apache.celeborn.CelebornFunSuite
import org.apache.celeborn.client.WithShuffleClientSuite

// TODO need add shuffle client test
class DeploySuite extends CelebornFunSuite with WithMiniKube {
  final val masterStatefulSet =
    kubernetesClient.apps().statefulSets().withName("celeborn-master").get()
  assert(masterStatefulSet != null)
  final val workerStatefulSet =
    kubernetesClient.apps().statefulSets().withName("celeborn-worker").get()
  assert(workerStatefulSet != null)

  test("Check master started") {
    val masterPod = kubernetesClient.pods().withName("celeborn-master-0")
    // wait for master ready
    eventually(timeout(5 minutes), interval(10 seconds)) {
      val log = masterPod.getLog(true)
      assert(log.contains("Master started."))
    }

    assert(masterPod.get().getStatus.getPhase == "Running")
  }

  test("Check worker started") {
    val workerPod = kubernetesClient.pods().withName("celeborn-worker-0")
    // wait for worker ready
    eventually(timeout(5 minutes), interval(10 seconds)) {
      val log = workerPod.getLog(true)
      assert(log.contains("Register worker successfully."))
    }

    assert(workerPod.get().getStatus.getPhase == "Running")
  }
}
