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

package za.co.absa.spline.harvester.conf

import org.apache.commons.configuration.MapConfiguration
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.spline.agent.AgentConfig.ConfProperty
import za.co.absa.spline.harvester.conf.Spline05ConfigurationAdapterTest._

import scala.collection.JavaConverters.{asScalaIteratorConverter, mapAsJavaMapConverter}


class Spline05ConfigurationAdapterTest extends AnyFlatSpec with Matchers {
  behavior of "Spline05ConfigurationAdapter"

  it should "be empty if no deprecated keys are present in the underlying config" in {
    val config = createConfigAdapter(Map(
      ConfProperty.IgnoreWriteDetectionStrategy -> "foo"
    ))

    config.isEmpty shouldEqual true
  }

  it should "not be empty if deprecated keys are present in the underlying config" in {
    val config = createConfigAdapter(Map(
      "spline.iwd_strategy.className" -> "foo.bar.Baz.class"
    ))

    config.isEmpty shouldEqual false
  }

  it should "remap the key and return the value from the underlying config only if present" in {
    testConfig.containsKey(ConfProperty.IgnoreWriteDetectionStrategy) shouldEqual true
    testConfig.getProperty(ConfProperty.IgnoreWriteDetectionStrategy) shouldEqual "default"
  }

  it should "return only the keys that have deprecated counterparts in the underlying config" in {
    val keys = testConfig.getKeys.asScala.toSeq

    keys.size shouldEqual 2
    keys.contains(ConfProperty.IgnoreWriteDetectionStrategy) shouldEqual true
    keys.contains(s"${ConfProperty.IgnoreWriteDetectionStrategy}.default.className") shouldEqual true
  }

  it should "contain dispatcher name only if the dispatcher classname is defined as well" in {
    val config = createConfigAdapter(Map(
      "spline.lineage_dispatcher.className" -> "foo.bar.Baz.class"
    ))

    config.containsKey(ConfProperty.RootLineageDispatcher) shouldEqual true
    config.getProperty(ConfProperty.RootLineageDispatcher) shouldEqual "http"
    config.getProperty(s"${ConfProperty.RootLineageDispatcher}.http.className") shouldEqual "foo.bar.Baz.class"

    val keys = config.getKeys.asScala.toSeq
    keys.contains(ConfProperty.RootLineageDispatcher) shouldEqual true

    val emptyConfig = createConfigAdapter(Map.empty)

    emptyConfig.containsKey(ConfProperty.RootLineageDispatcher) shouldEqual false
  }

}

object Spline05ConfigurationAdapterTest {

  private def createConfigAdapter(map: Map[String, Any]) =
    new Spline05ConfigurationAdapter(new MapConfiguration(map.asJava))

  private val testConfig = createConfigAdapter(Map(
    "spline.iwd_strategy.className" -> "foo.bar.Baz.class",
    ConfProperty.Mode -> "BEST_EFFORT"
  ))
}
