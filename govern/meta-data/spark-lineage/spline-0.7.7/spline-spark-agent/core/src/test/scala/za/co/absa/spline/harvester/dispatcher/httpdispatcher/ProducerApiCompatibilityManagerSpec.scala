/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester.dispatcher.httpdispatcher

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.commons.version.Version._
import za.co.absa.spline.harvester.dispatcher.ProducerApiVersion.SupportedApiRange

class ProducerApiCompatibilityManagerSpec extends AnyFlatSpec with Matchers {

  "newerServerApiVersion()" should "return the highest server API version if one is greater than one supported by the agent" in {
    ProducerApiCompatibilityManager(Seq(ver"888", ver"999", ver"777"), Nil).newerServerApiVersion should equal(Some(ver"999"))
    ProducerApiCompatibilityManager(Seq(SupportedApiRange.Max), Nil).newerServerApiVersion should equal(None)
    ProducerApiCompatibilityManager(Seq(SupportedApiRange.Min), Nil).newerServerApiVersion should equal(None)
    ProducerApiCompatibilityManager(Nil, Nil).newerServerApiVersion should equal(None)
  }

  "highestCompatibleApiVersion()" should "return highest version that is supported by both agent and server" in {
    ProducerApiCompatibilityManager(Seq(SupportedApiRange.Max), Nil).highestCompatibleApiVersion should equal(Some(SupportedApiRange.Max))
    ProducerApiCompatibilityManager(Seq(SupportedApiRange.Min), Nil).highestCompatibleApiVersion should equal(Some(SupportedApiRange.Min))
    ProducerApiCompatibilityManager(Seq(SupportedApiRange.Min, SupportedApiRange.Max), Nil).highestCompatibleApiVersion should equal(Some(SupportedApiRange.Max))
    ProducerApiCompatibilityManager(Seq(SupportedApiRange.Max, SupportedApiRange.Min), Nil).highestCompatibleApiVersion should equal(Some(SupportedApiRange.Max))
    ProducerApiCompatibilityManager(Seq(ver"0"), Nil).highestCompatibleApiVersion should equal(None)
    ProducerApiCompatibilityManager(Seq(ver"999"), Nil).highestCompatibleApiVersion should equal(None)
    ProducerApiCompatibilityManager(Nil, Nil).highestCompatibleApiVersion should equal(None)
  }

  "deprecatedApiVersion()" should "return the highest supported API version if one is deprecated" in {
    ProducerApiCompatibilityManager(Nil, Seq(ver"999")).deprecatedApiVersion should equal(Some(SupportedApiRange.Max))
    ProducerApiCompatibilityManager(Nil, Seq(SupportedApiRange.Min)).deprecatedApiVersion should equal(None)
    ProducerApiCompatibilityManager(Nil, Nil).deprecatedApiVersion should equal(None)
  }
}
