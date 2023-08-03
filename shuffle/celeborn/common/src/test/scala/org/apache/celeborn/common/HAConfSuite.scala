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

package org.apache.celeborn.common

import org.apache.celeborn.CelebornFunSuite

class HAConfSuite extends CelebornFunSuite {

  private def verifyConf(conf: CelebornConf): Unit = {
    assert(conf.haMasterNodeIds.sorted === Array("1", "2", "3"))

    assert(conf.haMasterNodeHost("1") === "clb-1")
    assert(conf.haMasterNodeHost("2") === "clb-2")
    assert(conf.haMasterNodeHost("3") === "clb-3")

    assert(conf.haMasterNodePort("1") === 10000)
    assert(conf.haMasterNodePort("2") === 20000)
    assert(conf.haMasterNodePort("3") === 30000)

    assert(conf.haMasterRatisHost("1") === "clb-1")
    assert(conf.haMasterRatisHost("2") === "clb-2")
    assert(conf.haMasterRatisHost("3") === "clb-3")

    assert(conf.haMasterRatisPort("1") === 11111)
    assert(conf.haMasterRatisPort("2") === 22222)
    assert(conf.haMasterRatisPort("3") === 33333)
  }

  val OLD_HA_PREFIX = "celeborn.ha.master"
  val NEW_HA_PREFIX = "celeborn.master.ha"

  test("extract masterNodeIds - old") {
    val conf = new CelebornConf()
      .set(s"$OLD_HA_PREFIX.node.id", "1")
      .set(s"$OLD_HA_PREFIX.node.1.host", "clb-1")
      .set(s"$OLD_HA_PREFIX.node.1.port", "10000")
      .set(s"$OLD_HA_PREFIX.node.1.ratis.port", "11111")
      .set(s"$OLD_HA_PREFIX.node.2.host", "clb-2")
      .set(s"$OLD_HA_PREFIX.node.2.port", "20000")
      .set(s"$OLD_HA_PREFIX.node.2.ratis.port", "22222")
      .set(s"$OLD_HA_PREFIX.node.3.host", "clb-3")
      .set(s"$OLD_HA_PREFIX.node.3.port", "30000")
      .set(s"$OLD_HA_PREFIX.node.3.ratis.port", "33333")
    verifyConf(conf)
  }

  test("extract masterNodeIds - new") {
    val conf = new CelebornConf()
      .set(s"$NEW_HA_PREFIX.node.id", "1")
      .set(s"$NEW_HA_PREFIX.node.1.host", "clb-1")
      .set(s"$NEW_HA_PREFIX.node.1.port", "10000")
      .set(s"$NEW_HA_PREFIX.node.1.ratis.port", "11111")
      .set(s"$NEW_HA_PREFIX.node.2.host", "clb-2")
      .set(s"$NEW_HA_PREFIX.node.2.port", "20000")
      .set(s"$NEW_HA_PREFIX.node.2.ratis.port", "22222")
      .set(s"$NEW_HA_PREFIX.node.3.host", "clb-3")
      .set(s"$NEW_HA_PREFIX.node.3.port", "30000")
      .set(s"$NEW_HA_PREFIX.node.3.ratis.port", "33333")
    verifyConf(conf)
  }

  test("extract masterNodeIds - mix") {
    val conf = new CelebornConf()
      .set(s"$NEW_HA_PREFIX.node.id", "1")
      .set(s"$OLD_HA_PREFIX.node.id", "invalid")
      .set(s"$NEW_HA_PREFIX.node.1.host", "clb-1")
      .set(s"$NEW_HA_PREFIX.node.1.port", "10000")
      .set(s"$NEW_HA_PREFIX.node.1.ratis.port", "11111")
      .set(s"$NEW_HA_PREFIX.node.2.host", "clb-2")
      .set(s"$NEW_HA_PREFIX.node.2.port", "20000")
      .set(s"$NEW_HA_PREFIX.node.2.ratis.port", "22222")
      .set(s"$OLD_HA_PREFIX.node.2.host", "invalid")
      .set(s"$OLD_HA_PREFIX.node.2.port", "44444")
      .set(s"$OLD_HA_PREFIX.node.2.ratis.port", "44444")
      .set(s"$OLD_HA_PREFIX.node.3.host", "clb-3")
      .set(s"$OLD_HA_PREFIX.node.3.port", "30000")
      .set(s"$OLD_HA_PREFIX.node.3.ratis.port", "33333")
    verifyConf(conf)
  }
}
