/*
 * Copyright 2021 ABSA Group Limited
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

package za.co.absa.spline.agent

import org.apache.commons.configuration.BaseConfiguration
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.spline.agent.AgentConfig.ConfProperty
import za.co.absa.spline.harvester.conf.{SQLFailureCaptureMode, SplineMode}
import za.co.absa.spline.harvester.dispatcher.LineageDispatcher
import za.co.absa.spline.harvester.iwd.IgnoredWriteDetectionStrategy
import za.co.absa.spline.harvester.postprocessing.PostProcessingFilter

import scala.collection.JavaConverters._

class AgentConfigSpec
  extends AnyFlatSpec
    with Matchers
    with MockitoSugar {

  "empty()" should "return empty config" in {
    val c1 = AgentConfig.empty
    val c2 = AgentConfig.empty

    Seq(c1, c2) foreach (_ should be(empty))
    c1 should not be theSameInstanceAs(c2)
  }

  "from(pairs)" should "create config from key-value pairs" in {
    val config = AgentConfig.from(Map("a" -> 1, "b" -> "foo"))
    config should not be empty
    config.getKeys.asScala should have length 2
    config.getProperty("a") should equal(1)
    config.getProperty("b") should equal("foo")
  }

  "from(configuration)" should "create config from Configuration" in {
    val agentConfig = AgentConfig.from(
      new BaseConfiguration {
        addProperty("a", 1)
        addProperty("b", "foo")
      })

    agentConfig should not be empty
    agentConfig.getKeys.asScala should have length 2
    agentConfig.getProperty("a") should equal(1)
    agentConfig.getProperty("b") should equal("foo")
  }

  behavior of "builder()"

  it should "build empty config" in {
    val config = AgentConfig
      .builder()
      .build()

    config should be(empty)
  }

  it should "add properties" in {
    val config = AgentConfig
      .builder()
      .config("a", 42)
      .config("b", "bbb")
      .config(Map("x" -> 1, "y" -> true))
      .build()

    config should not be empty
    config.getKeys.asScala should have length 4
    config.getProperty("a") should equal(42)
    config.getProperty("b") should equal("bbb")
    config.getProperty("x") should equal(1)
    config.getProperty("y") shouldBe true
  }

  it should "add enums" in {
    val config = AgentConfig
      .builder()
      .splineMode(SplineMode.BEST_EFFORT)
      .sqlFailureCaptureMode(SQLFailureCaptureMode.ALL)
      .build()

    config should not be empty
    config.getKeys.asScala should have length 2
    config.getProperty(ConfProperty.Mode) should equal(SplineMode.BEST_EFFORT.name)
    config.getProperty(ConfProperty.SQLFailureCaptureMode) should equal(SQLFailureCaptureMode.ALL.name)
  }

  it should "add objects" in {
    val mockDispatcher = mock[LineageDispatcher]
    val mockFilter = mock[PostProcessingFilter]
    val mockIwdStrategy = mock[IgnoredWriteDetectionStrategy]

    val config = AgentConfig
      .builder()
      .lineageDispatcher(mockDispatcher)
      .postProcessingFilter(mockFilter)
      .ignoredWriteDetectionStrategy(mockIwdStrategy)
      .build()

    config should not be empty
    config.getKeys.asScala should have length 3
    config.getProperty(ConfProperty.RootLineageDispatcher) should be theSameInstanceAs mockDispatcher
    config.getProperty(ConfProperty.RootPostProcessingFilter) should be theSameInstanceAs mockFilter
    config.getProperty(ConfProperty.IgnoreWriteDetectionStrategy) should be theSameInstanceAs mockIwdStrategy
  }

}
