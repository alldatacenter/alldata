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

package org.apache.griffin.measure.datasource.connector.batch

import scala.collection.mutable.{Map => MutableMap}
import scala.util._

import org.apache.spark.sql.{DataFrame, DataFrameReader, SparkSession}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types.StructType

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.dqdefinition.DataConnectorParam
import org.apache.griffin.measure.context.TimeRange
import org.apache.griffin.measure.datasource.TimestampStorage
import org.apache.griffin.measure.utils.HdfsUtil
import org.apache.griffin.measure.utils.ParamUtil._

/**
 * A batch data connector for file based sources which allows support various
 * file based data sources like Parquet, CSV, TSV, ORC etc.
 * Local files can also be read by prepending `file://` namespace.
 *
 * Currently supported formats like Parquet, ORC, AVRO, Text and Delimited types like CSV, TSV etc.
 *
 * Supported Configurations:
 *  - format : [[String]] specifying the type of file source (parquet, orc, etc.).
 *  - paths : [[Seq]] specifying the paths to be read
 *  - options : [[Map]] of format specific options
 *  - skipOnError : [[Boolean]] specifying whether to continue execution if one or more paths are invalid.
 *  - schema : [[Seq]] of {colName, colType and isNullable} given as key value pairs. If provided, this can
 * help skip the schema inference step for some underlying data sources.
 *
 * Some defaults assumed by this connector (if not set) are as follows:
 *  - `delimiter` is \t for TSV format,
 *  - `schema` is None,
 *  - `header` is false,
 *  - `format` is parquet
 */
case class FileBasedDataConnector(
    @transient sparkSession: SparkSession,
    dcParam: DataConnectorParam,
    timestampStorage: TimestampStorage)
    extends BatchDataConnector {

  import FileBasedDataConnector._

  val config: Map[String, Any] = dcParam.getConfig
  val options: MutableMap[String, String] = MutableMap(
    config.getParamStringMap(Options, Map.empty).toSeq: _*)

  var format: String = config.getString(Format, DefaultFormat).toLowerCase
  val paths: Seq[String] = config.getStringArr(Paths, Nil)
  val schemaSeq: Seq[Map[String, String]] =
    config.getAnyRef[Seq[Map[String, String]]](Schema, Nil)
  val skipErrorPaths: Boolean = config.getBoolean(SkipErrorPaths, defValue = false)

  val currentSchema: Option[StructType] = Try(getUserDefinedSchema) match {
    case Success(structType) if structType.fields.nonEmpty => Some(structType)
    case _ => None
  }

  assert(
    SupportedFormats.contains(format),
    s"Invalid format '$format' specified. Must be one of ${SupportedFormats.mkString("['", "', '", "']")}")

  // Use old implementation for AVRO format if current spark version is not 2.4.x and above
  if ("avro".equalsIgnoreCase(format) && sparkSession.version < "2.4.0") {
    format = "com.databricks.spark.avro"
  }

  if (format == "csv") {
    validateCSVOptions()
  }

  if (format == "tsv") {
    format = "csv"
    options.getOrElseUpdate(Delimiter, TabDelimiter)
  }

  /**
   * Builds a [[StructType]] from the given schema string provided as `Schema` config.
   *
   * @example
   * {"schema":[{"name":"user_id","type":"string","nullable":"true"},{"name":"age","type":"int","nullable":"false"}]}
   * {"schema":[{"name":"user_id","type":"decimal(5,2)","nullable":"true"}]}
   * {"schema":[{"name":"my_struct","type":"struct<f1:int,f2:string>","nullable":"true"}]}
   * @return
   */
  private def getUserDefinedSchema: StructType = {
    schemaSeq.foldLeft(new StructType())((currentStruct, fieldMap) => {
      val colName = fieldMap(ColName).toLowerCase
      val colType = fieldMap(ColType).toLowerCase
      val isNullable = Try(fieldMap(IsNullable).toLowerCase.toBoolean).getOrElse(true)

      currentStruct.add(colName, colType, isNullable)
    })
  }

  /**
   * Ensures the presence of schema either via `header` or `schema` options.
   *
   *  - If both are present, the preference will be given to `schema`. First row will be omitted
   * if `header` is set to true, else will be included.
   *  - If `schema` is defined, it must be valid.
   *  - If neither is set, a fatal exception is thrown.
   */
  private def validateCSVOptions(): Unit = {
    if (options.contains(Header) && config.contains(Schema)) {
      griffinLogger.warn(
        s"Both $Options.$Header and $Schema were provided. Defaulting to provided $Schema")
    }

    if (!options.contains(Header) && !config.contains(Schema)) {
      throw new IllegalArgumentException(
        s"Either '$Header' must be set in '$Options' or '$Schema' must be set.")
    }

    if (config.contains(Schema) && (schemaSeq.isEmpty || currentSchema.isEmpty)) {
      throw new IllegalStateException("Unable to create schema from specification")

    }
  }

  def data(ms: Long): (Option[DataFrame], TimeRange) = {
    val validPaths = getValidPaths(paths, skipErrorPaths)

    val dfOpt = {
      val dfOpt = Try(
        sparkSession.read
          .options(options)
          .format(format)
          .withSchemaIfAny(currentSchema)
          .load(validPaths: _*))

      dfOpt match {
        case Success(_) =>
        case Failure(exception) =>
          griffinLogger.error("Error occurred while reading data set.", exception)
      }

      val preDfOpt = preProcess(dfOpt.toOption, ms)
      preDfOpt
    }

    (dfOpt, TimeRange(ms, readTmst(ms)))
  }
}

object FileBasedDataConnector extends Loggable {
  private val Format: String = "format"
  private val Paths: String = "paths"
  private val Options: String = "options"
  private val SkipErrorPaths: String = "skipErrorPaths"
  private val Schema: String = "schema"
  private val Header: String = "header"
  private val Delimiter: String = "delimiter"

  private val ColName: String = "name"
  private val ColType: String = "type"
  private val IsNullable: String = "nullable"
  private val TabDelimiter: String = "\t"

  private val DefaultFormat: String = SQLConf.DEFAULT_DATA_SOURCE_NAME.defaultValueString
  private val SupportedFormats: Seq[String] =
    Seq("parquet", "orc", "avro", "text", "csv", "tsv", "com.databricks.spark.avro")

  /**
   * Validates the existence of paths in a given sequence.
   * Set option `skipOnError` to true to avoid fatal errors if any erroneous paths are encountered.
   *
   * @param paths       given sequence of paths
   * @param skipOnError flag to skip erroneous paths if any
   * @return
   */
  private def getValidPaths(paths: Seq[String], skipOnError: Boolean): Seq[String] = {
    val validPaths = paths.filter(
      path =>
        if (HdfsUtil.existPath(path)) true
        else {
          val msg = s"Path '$path' does not exist!"
          if (skipOnError) griffinLogger.error(msg)
          else throw new IllegalArgumentException(msg)

          false
        })

    assert(validPaths.nonEmpty, "No paths were given for the data source.")
    validPaths
  }

  /**
   * Adds methods implicitly to [[DataFrameReader]]
   *
   * @param dfr an instance of [[DataFrameReader]]
   */
  implicit class Implicits(dfr: DataFrameReader) {

    /**
     * Applies a schema to this [[DataFrameReader]] if any.
     *
     * @param schemaOpt an optional Schema
     * @return
     */
    def withSchemaIfAny(schemaOpt: Option[StructType]): DataFrameReader = {
      schemaOpt match {
        case Some(structType) => dfr.schema(structType)
        case None => dfr
      }
    }
  }

}
