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

package org.apache.griffin.measure.configuration.dqdefinition.reader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import org.apache.griffin.measure.configuration.dqdefinition._

class ParamEnumReaderSpec extends AnyFlatSpec with Matchers {
  import org.apache.griffin.measure.configuration.enums.DslType._
  "dsltype" should "be parsed to predefined set of values" in {
    val validDslSparkSqlValues =
      Seq("spark-sql", "spark-SQL", "SPARK-SQL", "sparksql")
    validDslSparkSqlValues foreach { x =>
      val ruleParam = RuleParam(x, "accuracy")
      ruleParam.getDslType should ===(SparkSql)
    }
    val invalidDslSparkSqlValues = Seq("spark", "sql", "")
    invalidDslSparkSqlValues foreach { x =>
      val ruleParam = RuleParam(x, "accuracy")
      ruleParam.getDslType should not be SparkSql
    }

    val validDslGriffinValues =
      Seq("griffin-dsl", "griffindsl", "griFfin-dsl", "")
    validDslGriffinValues foreach { x =>
      val ruleParam = RuleParam(x, "accuracy")
      ruleParam.getDslType should ===(GriffinDsl)
    }

  }

  "griffindsl" should "be returned as default dsl type" in {
    import org.apache.griffin.measure.configuration.enums.DslType._
    val dslGriffinDslValues = Seq("griffin", "dsl")
    dslGriffinDslValues foreach { x =>
      val ruleParam = RuleParam(x, "accuracy")
      ruleParam.getDslType should be(GriffinDsl)
    }
  }

  "dqtype" should "be parsed to predefined set of values" in {
    import org.apache.griffin.measure.configuration.enums.DqType._
    var ruleParam = RuleParam("griffin-dsl", "accuracy")
    ruleParam.getDqType should be(Accuracy)
    ruleParam = RuleParam("griffin-dsl", "accu")
    ruleParam.getDqType should not be Accuracy
    ruleParam.getDqType should be(Unknown)

    ruleParam = RuleParam("griffin-dsl", "profiling")
    ruleParam.getDqType should be(Profiling)
    ruleParam = RuleParam("griffin-dsl", "profilin")
    ruleParam.getDqType should not be Profiling
    ruleParam.getDqType should be(Unknown)

    ruleParam = RuleParam("griffin-dsl", "TIMELINESS")
    ruleParam.getDqType should be(Timeliness)
    ruleParam = RuleParam("griffin-dsl", "timeliness ")
    ruleParam.getDqType should not be Timeliness
    ruleParam.getDqType should be(Unknown)

    ruleParam = RuleParam("griffin-dsl", "UNIQUENESS")
    ruleParam.getDqType should be(Uniqueness)
    ruleParam = RuleParam("griffin-dsl", "UNIQUE")
    ruleParam.getDqType should not be Uniqueness
    ruleParam.getDqType should be(Unknown)

    ruleParam = RuleParam("griffin-dsl", "Duplicate")
    ruleParam.getDqType should be(Uniqueness)
    ruleParam = RuleParam("griffin-dsl", "duplica")
    ruleParam.getDqType should not be Duplicate
    ruleParam.getDqType should be(Unknown)

    ruleParam = RuleParam("griffin-dsl", "COMPLETENESS")
    ruleParam.getDqType should be(Completeness)
    ruleParam = RuleParam("griffin-dsl", "complete")
    ruleParam.getDqType should not be Completeness
    ruleParam.getDqType should be(Unknown)

    ruleParam = RuleParam("griffin-dsl", "")
    ruleParam.getDqType should be(Unknown)
    ruleParam = RuleParam("griffin-dsl", "duplicate")
    ruleParam.getDqType should not be Unknown
  }

  "outputtype" should "be valid" in {
    import org.apache.griffin.measure.configuration.enums.OutputType._
    var ruleOutputParam = RuleOutputParam("metric", "", "map")
    ruleOutputParam.getOutputType should be(MetricOutputType)
    ruleOutputParam = RuleOutputParam("metr", "", "map")
    ruleOutputParam.getOutputType should not be MetricOutputType
    ruleOutputParam.getOutputType should be(UnknownOutputType)

    ruleOutputParam = RuleOutputParam("record", "", "map")
    ruleOutputParam.getOutputType should be(RecordOutputType)
    ruleOutputParam = RuleOutputParam("rec", "", "map")
    ruleOutputParam.getOutputType should not be RecordOutputType
    ruleOutputParam.getOutputType should be(UnknownOutputType)

    ruleOutputParam = RuleOutputParam("dscupdate", "", "map")
    ruleOutputParam.getOutputType should be(DscUpdateOutputType)
    ruleOutputParam = RuleOutputParam("dsc", "", "map")
    ruleOutputParam.getOutputType should not be DscUpdateOutputType
    ruleOutputParam.getOutputType should be(UnknownOutputType)

  }

  "flattentype" should "be valid" in {
    import org.apache.griffin.measure.configuration.enums.FlattenType._
    var ruleOutputParam = RuleOutputParam("metric", "", "map")
    ruleOutputParam.getFlatten should be(MapFlattenType)
    ruleOutputParam = RuleOutputParam("metric", "", "metr")
    ruleOutputParam.getFlatten should not be MapFlattenType
    ruleOutputParam.getFlatten should be(DefaultFlattenType)

    ruleOutputParam = RuleOutputParam("metric", "", "array")
    ruleOutputParam.getFlatten should be(ArrayFlattenType)
    ruleOutputParam = RuleOutputParam("metric", "", "list")
    ruleOutputParam.getFlatten should be(ArrayFlattenType)
    ruleOutputParam = RuleOutputParam("metric", "", "arrays")
    ruleOutputParam.getFlatten should not be ArrayFlattenType
    ruleOutputParam.getFlatten should be(DefaultFlattenType)

    ruleOutputParam = RuleOutputParam("metric", "", "entries")
    ruleOutputParam.getFlatten should be(EntriesFlattenType)
    ruleOutputParam = RuleOutputParam("metric", "", "entry")
    ruleOutputParam.getFlatten should not be EntriesFlattenType
    ruleOutputParam.getFlatten should be(DefaultFlattenType)
  }

  "sinktype" should "be valid" in {
    import org.mockito.Mockito._

    import org.apache.griffin.measure.configuration.enums.SinkType._
    var dqConfig = DQConfig(
      "test",
      1234,
      "",
      Nil,
      mock(classOf[EvaluateRuleParam]),
      Nil,
      List(
        "Console",
        "Log",
        "CONSOLE",
        "LOG",
        "Es",
        "ElasticSearch",
        "Http",
        "MongoDB",
        "mongo",
        "hdfs"))
    dqConfig.getValidSinkTypes should be(Seq(Console, ElasticSearch, MongoDB, Hdfs))
    dqConfig = DQConfig(
      "test",
      1234,
      "",
      Nil,
      mock(classOf[EvaluateRuleParam]),
      Nil,
      List("Consol", "Logg"))
    dqConfig.getValidSinkTypes should not be Seq(Console)
    dqConfig.getValidSinkTypes should be(Seq())

    dqConfig = DQConfig("test", 1234, "", Nil, mock(classOf[EvaluateRuleParam]), Nil, List(""))
    dqConfig.getValidSinkTypes should be(Nil)
  }

}
