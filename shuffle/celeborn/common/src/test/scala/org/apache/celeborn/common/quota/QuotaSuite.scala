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

package org.apache.celeborn.common.quota

import org.apache.celeborn.common.identity.UserIdentifier

class QuotaSuite extends BaseQuotaManagerSuite {

  val quota1 = Quota(-1, -1, 300, 400)
  val quota2 = Quota(100, 200, 300, 400)
  val user1 = UserIdentifier("mock", "mock")
  val resourceConsumption1 = ResourceConsumption(10, 20, 30, 40)
  val resourceConsumption2 = ResourceConsumption(10, 20, 600, 800)
  val resourceConsumption3 = ResourceConsumption(1000, 2000, 3000, 4000)

  test("test check quota return result") {
    val res1 = quota1.checkQuotaSpaceAvailable(user1, resourceConsumption1)
    val res2 = quota1.checkQuotaSpaceAvailable(user1, resourceConsumption2)
    val res3 = quota2.checkQuotaSpaceAvailable(user1, resourceConsumption1)
    val res4 = quota2.checkQuotaSpaceAvailable(user1, resourceConsumption2)
    val res5 = quota2.checkQuotaSpaceAvailable(user1, resourceConsumption3)

    val exp1 = (true, "")
    val exp2 = (
      false,
      "User `mock`.`mock` used hdfsBytesWritten(600.0 B) exceeds quota(300.0 B). " +
        "User `mock`.`mock` used hdfsFileCount(800) exceeds quota(400). ")
    val exp3 = (true, "")
    val exp4 = (
      false,
      "User `mock`.`mock` used hdfsBytesWritten(600.0 B) exceeds quota(300.0 B). " +
        "User `mock`.`mock` used hdfsFileCount(800) exceeds quota(400). ")
    val exp5 = (
      false,
      "User `mock`.`mock` used diskBytesWritten (1000.0 B) exceeds quota (100.0 B). " +
        "User `mock`.`mock` used diskFileCount(2000) exceeds quota(200). " +
        "User `mock`.`mock` used hdfsBytesWritten(2.9 KiB) exceeds quota(300.0 B). " +
        "User `mock`.`mock` used hdfsFileCount(4000) exceeds quota(400). ")

    assert(res1 == exp1)
    assert(res2 == exp2)
    assert(res3 == exp3)
    assert(res4 == exp4)
    assert(res5 == exp5)
  }
}
