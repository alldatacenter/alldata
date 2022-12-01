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

package za.co.absa.spline.harvester.postprocessing.extra

import org.apache.commons.configuration.BaseConfiguration
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import za.co.absa.commons.scalatest.EnvFixture
import za.co.absa.spline.harvester.postprocessing.metadata.MetadataCollectingFilter._
import za.co.absa.spline.harvester.postprocessing.metadata.{BaseNodeName, MetadataCollectingFilter}
import za.co.absa.spline.harvester.{HarvestingContext, IdGeneratorsBundle}
import za.co.absa.spline.producer.model._

import java.util.UUID

class MetadataCollectingFilterSpec extends AnyFlatSpec with EnvFixture with Matchers with MockitoSugar {

  private val logicalPlan = mock[LogicalPlan]
  private val sparkSession = SparkSession.builder
    .master("local")
    .config("spark.ui.enabled", "false")
    .config("k", "nice")
    .getOrCreate()

  private val idGenerators = mock[IdGeneratorsBundle]
  private val harvestingContext = new HarvestingContext(logicalPlan, None, sparkSession, idGenerators)

  private val wop = WriteOperation("foo", append = false, "42", None, Seq.empty, None, None)
  private val nav = NameAndVersion("foo", "bar")
  private val defaultExtra = Some(Map("ttt" -> 777))
  private val ep = ExecutionPlan(None, Some("pn"), None, None, Operations(wop, None, None), None, None, nav, None, defaultExtra)
  private val ee = ExecutionEvent(UUID.randomUUID(), None, 66L, None, None, None, Some(
    Map("foo" -> "a", "bar" -> false, "baz" -> Seq(1, 2, 3))))

  behavior of "ExtraMetadataCollectingFilter"

  it should "parse and replace all variables with values" in {
    val configString =
      """
        |{
        |    "executionPlan": {
        |        "extra": {
        |            "qux": 42,
        |            "seq": [ "aaa", "bbb", "ccc" ],
        |            "foo": { "$js": "executionPlan.name()" },
        |            "bar": { "$env": "BAR_HOME" },
        |            "baz": { "$jvm": "some.jvm.prop" },
        |            "daz": { "$js": "session.conf().get('k')" }
        |       }
        |    }
        |}
        |""".stripMargin

    System.setProperty("some.jvm.prop", "123")
    setEnv("BAR_HOME", "rabbit")

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    val filter = new MetadataCollectingFilter(config)

    val processedPlan = filter.processExecutionPlan(ep, harvestingContext)

    val extra = processedPlan.extraInfo.get
    extra("ttt") shouldBe 777
    extra("qux") shouldBe 42
    extra("seq") shouldBe Seq("aaa", "bbb", "ccc")
    extra("foo") shouldBe Some("pn")
    extra("bar") shouldBe "rabbit"
    extra("baz") shouldBe "123"
    extra("daz") shouldBe "nice"
  }

  it should "support labels" in {
    val configString =
      """
        |{
        |    "executionPlan": {
        |        "labels": {
        |            "qux": 42,
        |            "tags": [ "aaa", "bbb", null, "ccc" ],
        |            "foo1": null,
        |            "foo2": [],
        |            "foo3": [null],
        |            "notFlat": ["a", ["b", "c"]],
        |            "bar": { "$env": "BAR_HOME" },
        |            "baz": { "$jvm": "some.jvm.prop" },
        |            "daz": { "$js": "session.conf().get('k')" }
        |        }
        |    },
        |    "executionEvent": {
        |        "labels": {
        |            "qux": 42
        |        }
        |    }
        |}
        |""".stripMargin

    System.setProperty("some.jvm.prop", "123")
    setEnv("BAR_HOME", "rabbit")

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    val filter = new MetadataCollectingFilter(config)

    val processedPlan = filter.processExecutionPlan(ep, harvestingContext)
    val processedEvent = filter.processExecutionEvent(ee, harvestingContext)

    processedPlan.labels shouldEqual Some(Map(
      "qux" -> Seq("42"),
      "tags" -> Seq("aaa", "bbb", "ccc"),
      "notFlat" -> Seq("a", "b", "c"),
      "bar" -> Seq("rabbit"),
      "baz" -> Seq("123"),
      "daz" -> Seq("nice")
    ))

    processedEvent.labels shouldEqual Some(Map(
      "qux" -> Seq("42")
    ))
  }

  it should "handle missing JSON property" in {
    val filter = new MetadataCollectingFilter(Map.empty[BaseNodeName.Type, Seq[RuleDef]])
    filter.processExecutionPlan(mock[ExecutionPlan], mock[HarvestingContext]) should not be null
  }

  it should "handle json nesting" in {
    val configString =
      """
        |{
        |    "executionEvent": {
        |        "extra": {
        |            "tux": {
        |               "qux": 42,
        |               "qax": {
        |                   "tax": { "$js": "session.conf().get('k')" }
        |               }
        |            }
        |        }
        |    }
        |}
        |""".stripMargin

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    val filter = new MetadataCollectingFilter(config)

    val processedEvent = filter.processExecutionEvent(ee, harvestingContext)

    val extra = processedEvent.extra.get
    val tux = extra("tux").asInstanceOf[Map[String, Any]]
    tux("qux") shouldBe 42

    val qax = tux("qax").asInstanceOf[Map[String, Any]]
    qax("tax") shouldBe "nice"
  }

  it should "merge nested extra" in {
    val configString =
      """
        |{
        |    "executionEvent": {
        |        "extra": {
        |            "tux": {
        |               "qux": 42,
        |               "qax": ["ta", "pa"]
        |            }
        |        }
        |    },
        |    "executionEvent[true]": {
        |        "extra": {
        |            "tux": {
        |               "qax": ["da", "ta"],
        |               "fax": 33
        |            }
        |        }
        |    }
        |}
        |""".stripMargin

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    val filter = new MetadataCollectingFilter(config)

    val processedEvent = filter.processExecutionEvent(ee, harvestingContext)

    val extra = processedEvent.extra.get
    val tux = extra("tux").asInstanceOf[Map[String, Any]]
    tux("qux") shouldBe 42
    tux("qax") shouldBe Seq("ta", "pa", "da")
    tux("fax") shouldBe 33
  }

  it should "apply defined predicates" in {
    val configString =
      """
        |{
        |    "executionEvent[@.timestamp > 65]": {
        |        "extra": { "tux": 1 }
        |    },
        |    "executionEvent[@.extra['foo'] == 'a' && @.extra['bar'] == 'x']": {
        |        "extra": { "bux": 2 }
        |    },
        |    "executionEvent[@.extra['foo'] == 'a' && !@.extra['bar']]": {
        |        "extra": { "dux": 3 }
        |    },
        |    "executionEvent[@.extra['baz'][2] >= 3]": {
        |        "extra": { "mux": 4 }
        |    },
        |    "executionEvent[@.extra['baz'][2] < 3]": {
        |        "extra": { "fux": 5 }
        |    }
        |}
        |""".stripMargin

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    val filter = new MetadataCollectingFilter(config)

    val processedEvent = filter.processExecutionEvent(ee, harvestingContext)

    val extra = processedEvent.extra.get
    extra("tux") shouldBe 1
    extra.get("bux") shouldBe None
    extra("dux") shouldBe 3
    extra("mux") shouldBe 4
    extra.get("fux") shouldBe None
  }

  it should "allow predicates to access sparkContext config" in {
    val configString =
      """
        |{
        |    "executionEvent[session.sparkContext.conf['spark.ui.enabled'] == 'false']": {
        |        "extra": { "tux": 1 }
        |    }
        |}
        |""".stripMargin

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    val filter = new MetadataCollectingFilter(config)

    val processedEvent = filter.processExecutionEvent(ee, harvestingContext)

    val extra = processedEvent.extra.get
    extra("tux") shouldBe 1
  }

  it should "throw an exception when parsing labels on unexpected places" in {
    val configString =
      """
        |{
        |    "operation": {
        |        "labels": { "tux": 1 }
        |    }
        |}
        |""".stripMargin

    val config = new BaseConfiguration {
      addPropertyDirect(InjectRulesKey, configString)
    }

    (the [IllegalArgumentException] thrownBy new MetadataCollectingFilter(config)).getMessage should include("Labels are not supported")
  }

}
