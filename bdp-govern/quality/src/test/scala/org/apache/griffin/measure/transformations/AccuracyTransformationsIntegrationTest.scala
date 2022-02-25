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

package org.apache.griffin.measure.transformations

import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._

import org.apache.griffin.measure.SparkSuiteBase
import org.apache.griffin.measure.configuration.dqdefinition._
import org.apache.griffin.measure.configuration.enums.ProcessType.BatchProcessType
import org.apache.griffin.measure.context.{ContextId, DQContext}
import org.apache.griffin.measure.datasource.DataSourceFactory
import org.apache.griffin.measure.job.builder.DQJobBuilder

case class AccuracyResult(total: Long, miss: Long, matched: Long, matchedFraction: Double)

class AccuracyTransformationsIntegrationTest
    extends AnyFlatSpec
    with Matchers
    with SparkSuiteBase {
  private val EMPTY_PERSON_TABLE = "empty_person"
  private val PERSON_TABLE = "person"

  override def beforeAll(): Unit = {
    super.beforeAll()

    dropTables()
    createPersonTable()
    createEmptyPersonTable()

    spark.conf.set("spark.sql.crossJoin.enabled", "true")
  }

  override def afterAll(): Unit = {
    dropTables()
    super.afterAll()
  }

  "accuracy" should "basically work" in {
    checkAccuracy(
      sourceName = PERSON_TABLE,
      targetName = PERSON_TABLE,
      expectedResult = AccuracyResult(total = 2, miss = 0, matched = 2, matchedFraction = 1.0))
  }

  "accuracy" should "work with empty target" in {
    checkAccuracy(
      sourceName = PERSON_TABLE,
      targetName = EMPTY_PERSON_TABLE,
      expectedResult = AccuracyResult(total = 2, miss = 2, matched = 0, matchedFraction = 0.0))
  }

  "accuracy" should "work with empty source" in {
    checkAccuracy(
      sourceName = EMPTY_PERSON_TABLE,
      targetName = PERSON_TABLE,
      expectedResult = AccuracyResult(total = 0, miss = 0, matched = 0, matchedFraction = 1.0))
  }

  "accuracy" should "work with empty source and target" in {
    checkAccuracy(
      sourceName = EMPTY_PERSON_TABLE,
      targetName = EMPTY_PERSON_TABLE,
      expectedResult = AccuracyResult(total = 0, miss = 0, matched = 0, matchedFraction = 1.0))
  }

  private def checkAccuracy(
      sourceName: String,
      targetName: String,
      expectedResult: AccuracyResult) = {
    val dqContext: DQContext = getDqContext(dataSourcesParam = List(
      DataSourceParam(name = "source", connector = dataConnectorParam(tableName = sourceName)),
      DataSourceParam(name = "target", connector = dataConnectorParam(tableName = targetName))))
    dqContext.loadDataSources()

    val accuracyRule = RuleParam(
      dslType = "griffin-dsl",
      dqType = "ACCURACY",
      outDfName = "person_accuracy",
      rule = "source.name = target.name")

    val spark = this.spark
    import spark.implicits._
    val res = getRuleResults(dqContext, accuracyRule)
      .as[AccuracyResult]
      .collect()

    res.length shouldBe 1

    res(0) shouldEqual expectedResult
  }

  private def getRuleResults(dqContext: DQContext, rule: RuleParam): DataFrame = {
    val dqJob =
      DQJobBuilder.buildDQJob(dqContext, evaluateRuleParam = EvaluateRuleParam(List(rule)))

    dqJob.execute(dqContext)

    spark.sql(s"select * from ${rule.getOutDfName()}")
  }

  private def createPersonTable(): Unit = {
    val personCsvPath = getClass.getResource("/hive/person_table.csv").getFile

    spark.sql(
      s"CREATE TABLE $PERSON_TABLE " +
        "( " +
        "  name String," +
        "  age int " +
        ") " +
        "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' " +
        "STORED AS TEXTFILE")

    spark.sql(s"LOAD DATA LOCAL INPATH '$personCsvPath' OVERWRITE INTO TABLE $PERSON_TABLE")
  }

  private def createEmptyPersonTable(): Unit = {
    spark.sql(
      s"CREATE TABLE $EMPTY_PERSON_TABLE " +
        "( " +
        "  name String," +
        "  age int " +
        ") " +
        "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' " +
        "STORED AS TEXTFILE")

    spark.sql(s"select * from $EMPTY_PERSON_TABLE").show()
  }

  private def dropTables(): Unit = {
    spark.sql(s"DROP TABLE IF EXISTS $PERSON_TABLE ")
    spark.sql(s"DROP TABLE IF EXISTS $EMPTY_PERSON_TABLE ")
  }

  private def getDqContext(
      dataSourcesParam: Seq[DataSourceParam],
      name: String = "test-context"): DQContext = {
    val dataSources = DataSourceFactory.getDataSources(spark, null, dataSourcesParam)
    dataSources.foreach(_.init())

    DQContext(ContextId(System.currentTimeMillis), name, dataSources, Nil, BatchProcessType)(
      spark)
  }

  private def dataConnectorParam(tableName: String) = {
    DataConnectorParam(
      conType = "HIVE",
      dataFrameName = null,
      config = Map("table.name" -> tableName),
      preProc = null)
  }
}
