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

package org.apache.celeborn.service.deploy

import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.CelebornConf.WORKER_RPC_PORT
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.util.Utils
import org.apache.celeborn.service.deploy.worker.WorkerArguments

class WorkerArgumentsSuite extends AnyFunSuite with Logging {

  test("[CELEBORN-98] Test build workerArguments in different case") {
    val args1 = Array.empty[String]
    val conf1 = new CelebornConf()

    val arguments1 = new WorkerArguments(args1, conf1)
    assert(arguments1.host.equals(Utils.localHostName))
    assert(arguments1.port == 0)

    // should use celeborn conf
    val conf2 = new CelebornConf()
    conf2.set(WORKER_RPC_PORT, 12345)

    val arguments2 = new WorkerArguments(args1, conf2)
    assert(arguments2.port == 12345)

    // should use cli args
    val args2 = Array("-h", "test-host-1", "-p", "22345")

    val arguments3 = new WorkerArguments(args2, conf2)
    assert(arguments3.host.equals("test-host-1"))
    assert(arguments3.port == 22345)
  }
}
