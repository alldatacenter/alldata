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

package org.apache.griffin.measure.configuration.dqdefinition

import scala.util.Try

import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.fasterxml.jackson.annotation.JsonInclude.Include
import io.netty.util.internal.StringUtil
import org.apache.commons.lang.StringUtils

import org.apache.griffin.measure.configuration.enums._
import org.apache.griffin.measure.configuration.enums.DqType._
import org.apache.griffin.measure.configuration.enums.DslType.{DslType, GriffinDsl}
import org.apache.griffin.measure.configuration.enums.FlattenType.{
  DefaultFlattenType,
  FlattenType
}
import org.apache.griffin.measure.configuration.enums.OutputType.{OutputType, UnknownOutputType}
import org.apache.griffin.measure.configuration.enums.SinkType.SinkType

/**
 * dq param
 * @param name           name of dq measurement (must)
 * @param timestamp      default timestamp of measure in batch mode (optional)
 * @param procType       batch mode or streaming mode (must)
 * @param dataSources    data sources (must)
 * @param evaluateRule   dq measurement (must)
 * @param sinks          sink types (optional, by default will be elasticsearch)
 */
@JsonInclude(Include.NON_NULL)
case class DQConfig(
    @JsonProperty("name") private val name: String,
    @JsonProperty("timestamp") private val timestamp: Long,
    @JsonProperty("process.type") private val procType: String,
    @JsonProperty("data.sources") private val dataSources: List[DataSourceParam],
    @JsonProperty("evaluate.rule") private val evaluateRule: EvaluateRuleParam,
    @JsonProperty("measures") private val measures: Seq[MeasureParam],
    @JsonProperty("sinks") private val sinks: List[String] = Nil)
    extends Param {
  def getName: String = name
  def getTimestampOpt: Option[Long] = if (timestamp != 0) Some(timestamp) else None
  def getProcType: String = procType
  def getDataSources: Seq[DataSourceParam] = {
    dataSources
      .foldLeft((Nil: Seq[DataSourceParam], Set[String]())) { (ret, ds) =>
        val (seq, names) = ret
        if (!names.contains(ds.getName)) {
          (seq :+ ds, names + ds.getName)
        } else ret
      }
      ._1
  }
  def getEvaluateRule: EvaluateRuleParam = evaluateRule
  def getMeasures: Seq[MeasureParam] = measures
  def getSinkNames: Seq[String] = sinks
  def getValidSinkTypes: Seq[SinkType] = SinkType.validSinkTypes(sinks)

  def validate(): Unit = {
    assert(StringUtils.isNotBlank(name), "dq config name should not be blank")
    assert(StringUtils.isNotBlank(procType), "process.type should not be blank")
    assert(dataSources != null, "data.sources should not be null")
    getDataSources.foreach(_.validate())

    if (measures != null && measures.nonEmpty) {
      measures.foreach(_.validate())

      val repeatedMeasures = measures
        .map(_.getName)
        .groupBy(x => x)
        .mapValues(_.size)
        .filter(_._2 > 1)
        .keys

      assert(
        repeatedMeasures.isEmpty,
        "Measure names must be unique. " +
          s"Duplicate Measures names ['${repeatedMeasures.mkString("', '")}'] were found.")

      val invalidMeasureSources = measures
        .map(_.getDataSource)
        .map(dataSource => (dataSource, getDataSources.exists(_.getName.matches(dataSource))))
        .filterNot(_._2)
        .map(_._1)

      assert(
        invalidMeasureSources.isEmpty,
        "Measure source(s) undefined." +
          s" Unknown source(s) ['${invalidMeasureSources.mkString("', '")}'] were found.")
    } else if (evaluateRule != null) {
      evaluateRule.validate()
    } else {
      assert(
        assertion = false,
        "Either 'measure' or 'evaluate.rule' must be defined in the Application Config.")
    }
  }
}

trait ConfigParam extends Param {
  @JsonProperty("name")
  private val name: String = StringUtils.EMPTY

  @JsonProperty("type")
  private val configType: String = StringUtils.EMPTY

  @JsonProperty("config")
  private val config: Map[String, Any] = Map.empty

  def getName: String = name
  def getType: String = configType
  def getConfig: Map[String, Any] = config
}

case class MeasureParam(
    @JsonProperty("name") private val name: String,
    @JsonProperty("type") private val measureType: String,
    @JsonProperty("data.source") private val dataSource: String,
    @JsonProperty("config") private val config: Map[String, Any] = Map.empty,
    @JsonProperty("out") private val outputs: List[RuleOutputParam] = Nil)
    extends Param {

  def getName: String = name

  def getType: MeasureTypes.MeasureType = MeasureTypes.withNameWithDefault(measureType)

  def getConfig: Map[String, Any] = config

  def getDataSource: String = dataSource

  def getOutputs: Seq[RuleOutputParam] = if (outputs != null) outputs else Nil

  def getOutputOpt(tp: OutputType): Option[RuleOutputParam] =
    getOutputs.find(_.getOutputType == tp)

  override def validate(): Unit = {
    assert(!StringUtil.isNullOrEmpty(dataSource), "data.source should not be empty or null")
    assert(!getType.equals(MeasureTypes.Unknown), s"Unknown measure type '$measureType' provided")
  }
}

/**
 * data source param
 * @param name         data source name (must)
 * @param baseline     data source is baseline or not, false by default (optional)
 * @param connector   data connector (optional)
 * @param checkpoint   data source checkpoint configuration (must in streaming mode with streaming connectors)
 */
@JsonInclude(Include.NON_NULL)
case class DataSourceParam(
    @JsonProperty("name") private val name: String,
    @JsonProperty("connector") private val connector: DataConnectorParam,
    @JsonProperty("baseline") private val baseline: Boolean = false,
    @JsonProperty("checkpoint") private val checkpoint: Map[String, Any] = null)
    extends Param {
  def getName: String = name
  def isBaseline: Boolean = if (Option(baseline).isDefined) baseline else false
  def getConnector: Option[DataConnectorParam] = Option(connector)
  def getCheckpointOpt: Option[Map[String, Any]] = Option(checkpoint)

  def validate(): Unit = {
    assert(StringUtils.isNotBlank(name), "data source name should not be empty")
    assert(getConnector.isDefined, "Connector is undefined or invalid")
    getConnector.foreach(_.validate())
  }
}

/**
 * data connector param
 * @param conType    data connector type, e.g.: hive, avro, kafka (must)
 * @param dataFrameName    data connector dataframe name, for pre-process input usage (optional)
 * @param config     detail configuration of data connector (must)
 * @param preProc    pre-process rules after load data (optional)
 */
@JsonInclude(Include.NON_NULL)
case class DataConnectorParam(
    @JsonProperty("type") private val conType: String,
    @JsonProperty("dataframe.name") private val dataFrameName: String,
    @JsonProperty("config") private val config: Map[String, Any],
    @JsonProperty("pre.proc") private val preProc: List[String])
    extends Param {
  def getType: String = conType
  def getDataFrameName(defName: String): String =
    if (dataFrameName != null) dataFrameName else defName
  def getConfig: Map[String, Any] = if (config != null) config else Map[String, Any]()
  def getPreProcRules: Seq[String] = if (preProc != null) preProc else Nil

  def validate(): Unit = {
    assert(StringUtils.isNotBlank(conType), "data connector type should not be empty")
    getPreProcRules.forall(rule => !StringUtil.isNullOrEmpty(rule))
  }
}

/**
 * evaluate rule param
 * @param rules      rules to define dq measurement (optional)
 */
@JsonInclude(Include.NON_NULL)
case class EvaluateRuleParam(@JsonProperty("rules") private val rules: List[RuleParam])
    extends Param {
  def getRules: Seq[RuleParam] = if (rules != null) rules else Nil

  def validate(): Unit = {
    getRules.foreach(_.validate())
  }
}

/**
 * rule param
 * @param dslType    dsl type of this rule (must)
 * @param dqType     dq type of this rule (must if dsl type is "griffin-dsl")
 * @param inDfName   name of input dataframe of this rule, by default will be the previous rule output dataframe name
 * @param outDfName  name of output dataframe of this rule, by default will be generated
 *                   as data connector dataframe name with index suffix
 * @param rule       rule to define dq step calculation (must)
 * @param details    detail config of rule (optional)
 * @param cache      cache the result for multiple usage (optional, valid for "spark-sql" and "df-ops" mode)
 * @param outputs    output ways configuration (optional)
 * @param errorConfs error configuration (valid for 'COMPLETENESS' mode)
 */
@JsonInclude(Include.NON_NULL)
case class RuleParam(
    @JsonProperty("dsl.type") private val dslType: String,
    @JsonProperty("dq.type") private val dqType: String,
    @JsonProperty("in.dataframe.name") private val inDfName: String = null,
    @JsonProperty("out.dataframe.name") private val outDfName: String = null,
    @JsonProperty("rule") private val rule: String = null,
    @JsonProperty("details") private val details: Map[String, Any] = null,
    @JsonProperty("cache") private val cache: Boolean = false,
    @JsonProperty("out") private val outputs: List[RuleOutputParam] = null,
    @JsonProperty("error.confs") private val errorConfs: List[RuleErrorConfParam] = null)
    extends Param {
  def getDslType: DslType =
    if (dslType != null) DslType.withNameWithDefault(dslType) else GriffinDsl
  def getDqType: DqType = if (dqType != null) DqType.withNameWithDefault(dqType) else Unknown
  def getCache: Boolean = if (cache) cache else false

  def getInDfName(defName: String = ""): String = if (inDfName != null) inDfName else defName
  def getOutDfName(defName: String = ""): String = if (outDfName != null) outDfName else defName
  def getRule: String = if (rule != null) rule else ""
  def getDetails: Map[String, Any] = if (details != null) details else Map[String, Any]()

  def getOutputs: Seq[RuleOutputParam] = if (outputs != null) outputs else Nil
  def getOutputOpt(tp: OutputType): Option[RuleOutputParam] =
    getOutputs.find(_.getOutputType == tp)

  def getErrorConfs: Seq[RuleErrorConfParam] = if (errorConfs != null) errorConfs else Nil

  def validate(): Unit = {
    assert(!StringUtil.isNullOrEmpty(dqType), "DQ type cannot be null.")
    assert(Try(getDqType).isSuccess, s"Undefined measure type '$dqType' encountered.")

    getOutputs.foreach(_.validate())
    getErrorConfs.foreach(_.validate())
  }
}

/**
 * out param of rule
 * @param outputType     output type (must)
 * @param name           output name (optional)
 * @param flatten        flatten type of output metric (optional, available in output metric type)
 */
@JsonInclude(Include.NON_NULL)
case class RuleOutputParam(
    @JsonProperty("type") private val outputType: String,
    @JsonProperty("name") private val name: String,
    @JsonProperty("flatten") private val flatten: String)
    extends Param {
  def getOutputType: OutputType = {
    if (outputType != null) OutputType.withNameWithDefault(outputType)
    else UnknownOutputType
  }
  def getNameOpt: Option[String] = Some(name).filter(StringUtils.isNotBlank)
  def getFlatten: FlattenType = {
    if (StringUtils.isNotBlank(flatten)) FlattenType.withNameWithDefault(flatten)
    else DefaultFlattenType
  }

  def validate(): Unit = {}
}

/**
 * error configuration parameter
 * @param columnName the name of the column
 * @param errorType  the way to match error, regex or enumeration
 * @param values     error value list
 */
@JsonInclude(Include.NON_NULL)
case class RuleErrorConfParam(
    @JsonProperty("column.name") private val columnName: String,
    @JsonProperty("type") private val errorType: String,
    @JsonProperty("values") private val values: List[String])
    extends Param {
  def getColumnName: Option[String] = Some(columnName).filter(StringUtils.isNotBlank)
  def getErrorType: Option[String] = Some(errorType).filter(StringUtils.isNotBlank)
  def getValues: Seq[String] = if (values != null) values else Nil

  def validate(): Unit = {
    assert(
      "regex".equalsIgnoreCase(getErrorType.get) ||
        "enumeration".equalsIgnoreCase(getErrorType.get),
      "error error.conf type")
  }
}
