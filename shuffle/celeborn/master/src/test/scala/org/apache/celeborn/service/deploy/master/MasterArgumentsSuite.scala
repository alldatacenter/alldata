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

package org.apache.celeborn.service.deploy.master

import org.scalatest.funsuite.AnyFunSuite

import org.apache.celeborn.common.CelebornConf
import org.apache.celeborn.common.CelebornConf.{MASTER_HOST, MASTER_PORT}
import org.apache.celeborn.common.internal.Logging
import org.apache.celeborn.common.util.Utils

class MasterArgumentsSuite extends AnyFunSuite with Logging {

  test("[CELEBORN-98] Test build masterArguments in different case") {
    val args1 = Array.empty[String]
    val conf1 = new CelebornConf()

    val arguments1 = new MasterArguments(args1, conf1)
    assert(arguments1.host.equals(Utils.localHostName))
    assert(arguments1.port == 9097)

    // should use celeborn conf
    val conf2 = new CelebornConf()
    conf2.set(MASTER_HOST, "test-host-1")
    conf2.set(MASTER_PORT, 19097)

    val arguments2 = new MasterArguments(args1, conf2)
    assert(arguments2.host.equals("test-host-1"))
    assert(arguments2.port == 19097)

    // should use cli args
    val args2 = Array("-h", "test-host-2", "-p", "29097")

    val arguments3 = new MasterArguments(args2, conf2)
    assert(arguments3.host.equals("test-host-2"))
    assert(arguments3.port == 29097)
  }
}
