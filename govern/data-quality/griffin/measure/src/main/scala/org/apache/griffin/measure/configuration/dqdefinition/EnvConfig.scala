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

import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.fasterxml.jackson.annotation.JsonInclude.Include
import org.apache.commons.lang.StringUtils

import org.apache.griffin.measure.configuration.enums.SinkType
import org.apache.griffin.measure.configuration.enums.SinkType.SinkType

/**
 * Model for Environment Config.
 *
 * @param sparkParam Job specific Spark Configs to override the Defaults set on the cluster
 * @param sinkParams A [[Seq]] of sink definitions where records and metrics can be persisted
 * @param checkpointParams Config of checkpoint locations (required in streaming mode)
 */
@JsonInclude(Include.NON_NULL)
case class EnvConfig(
    @JsonProperty("spark") private val sparkParam: SparkParam,
    @JsonProperty("sinks") private val sinkParams: List[SinkParam],
    @JsonProperty("griffin.checkpoint") private val checkpointParams: List[CheckpointParam])
    extends Param {
  def getSparkParam: SparkParam = sparkParam
  def getSinkParams: Seq[SinkParam] = if (sinkParams != null) sinkParams else Nil
  def getCheckpointParams: Seq[CheckpointParam] =
    if (checkpointParams != null) checkpointParams else Nil

  def validate(): Unit = {
    assert(sparkParam != null, "spark param should not be null")
    sparkParam.validate()
    getSinkParams.foreach(_.validate())
    val repeatedSinks = sinkParams
      .map(_.getName)
      .groupBy(x => x)
      .mapValues(_.size)
      .filter(_._2 > 1)
      .keys
    assert(
      repeatedSinks.isEmpty,
      s"sink names must be unique. duplicate sink names ['${repeatedSinks.mkString("', '")}'] were found.")
    getCheckpointParams.foreach(_.validate())
  }
}

/**
 * spark param
 * @param logLevel         log level of spark application (optional)
 * @param cpDir            checkpoint directory for spark streaming (required in streaming mode)
 * @param batchInterval    batch interval for spark streaming (required in streaming mode)
 * @param processInterval  process interval for streaming dq calculation (required in streaming mode)
 * @param config           extra config for spark environment (optional)
 * @param initClear        clear checkpoint directory or not when initial (optional)
 */
@JsonInclude(Include.NON_NULL)
case class SparkParam(
    @JsonProperty("log.level") private val logLevel: String,
    @JsonProperty("checkpoint.dir") private val cpDir: String,
    @JsonProperty("batch.interval") private val batchInterval: String,
    @JsonProperty("process.interval") private val processInterval: String,
    @JsonProperty("config") private val config: Map[String, String],
    @JsonProperty("init.clear") private val initClear: Boolean)
    extends Param {
  def getLogLevel: String = if (logLevel != null) logLevel else "WARN"
  def getCpDir: String = if (cpDir != null) cpDir else ""
  def getBatchInterval: String = if (batchInterval != null) batchInterval else ""
  def getProcessInterval: String = if (processInterval != null) processInterval else ""
  def getConfig: Map[String, String] = if (config != null) config else Map[String, String]()
  def needInitClear: Boolean = if (initClear) initClear else false

  def validate(): Unit = {
//    assert(StringUtils.isNotBlank(cpDir), "checkpoint.dir should not be empty")
//    assert(TimeUtil.milliseconds(getBatchInterval).nonEmpty, "batch.interval should be valid time string")
//    assert(TimeUtil.milliseconds(getProcessInterval).nonEmpty, "process.interval should be valid time string")
  }
}

/**
 * sink param
 * @param sinkType       sink type, e.g.: log, hdfs, http, mongo (must)
 * @param config         config of sink way (must)
 */
@JsonInclude(Include.NON_NULL)
case class SinkParam(
    @JsonProperty("name") private val name: String,
    @JsonProperty("type") private val sinkType: String,
    @JsonProperty("config") private val config: Map[String, Any] = Map.empty)
    extends Param {
  def getName: String = name
  def getType: SinkType = SinkType.withNameWithDefault(sinkType)
  def getConfig: Map[String, Any] = config

  def validate(): Unit = {
    assert(name != null, "sink name should must be defined")
    assert(StringUtils.isNotBlank(sinkType), "sink type should not be empty")
  }
}

/**
 * checkpoint param
 * @param cpType       checkpoint location type, e.g.: zookeeper (must)
 * @param config       config of checkpoint location
 */
@JsonInclude(Include.NON_NULL)
case class CheckpointParam(
    @JsonProperty("type") private val cpType: String,
    @JsonProperty("config") private val config: Map[String, Any])
    extends Param {
  def getType: String = cpType
  def getConfig: Map[String, Any] = if (config != null) config else Map[String, Any]()

  def validate(): Unit = {
    assert(StringUtils.isNotBlank(cpType), "griffin checkpoint type should not be empty")
  }
}
