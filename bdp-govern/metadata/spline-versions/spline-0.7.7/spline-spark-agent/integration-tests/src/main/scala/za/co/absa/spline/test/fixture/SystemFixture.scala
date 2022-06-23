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

package za.co.absa.spline.test.fixture

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.collection.mutable

object SystemFixture {

  class PropsBackup[A, B](props: => mutable.Map[A, B]) {
    private var bkp: Map[A, B] = _

    def backup(): Unit = {
      this.bkp = props.toMap
    }

    def restore(): Unit = {
      props.retain((k, _) => this.bkp.contains(k))
      props ++= this.bkp
    }
  }

  trait IsolatedSystemPropertiesPerTest extends BeforeAndAfterEach with BeforeAndAfterAll {
    this: Suite =>

    private[this] val suitePropsBackup = new PropsBackup(sys.props)
    private[this] val testPropsBackup = new PropsBackup(sys.props)

    override protected def beforeAll(): Unit = {
      this.suitePropsBackup.backup()
      super.beforeEach()
    }

    override protected def afterAll(): Unit = {
      try super.afterEach()
      finally this.suitePropsBackup.restore()
    }

    override protected def beforeEach() {
      this.testPropsBackup.backup()
      super.beforeEach()
    }

    override protected def afterEach() {
      try super.afterEach()
      finally this.testPropsBackup.restore()
    }
  }

}

