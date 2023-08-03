package com.linkedin.feathr.offline.generation

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper
import com.linkedin.feathr.common.exception.{ErrorLabel, FeathrDataOutputException}
import com.linkedin.feathr.common.{Header, TaggedFeatureName}
import com.linkedin.feathr.offline.generation.FeatureDataHDFSProcessUtils._
import com.linkedin.feathr.offline.util.{HdfsUtils, SourceUtils}
import com.linkedin.feathr.offline.source.dataloader.DataLoaderHandler
import org.apache.avro.Schema
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.convert.wrapAll._

/**
 * write a dataframe to HDFS in FDS format for feature generation
 */
private[offline] object RawDataWriterUtils {

  /**
   * write a single dataframe of feature generation output to HDFS in name-term-value format
   *
   * @param featureHeaderMap a map of feature to its header information
   * @param parentPath         parent HDFS path to store the dataframe
   * @param outputParts optional output num parts
   * @param endTimeOpt         optional string of end time, in yyyy/MM/dd format
   * @param saveSchemaMeta save schema and meta data or not
   * @param dataLoaderHandlers additional data loader handlers that contain hooks for dataframe creation and manipulation
   */
  def writeFdsDataToDisk(
      ss: SparkSession,
      featureHeaderMap: Map[TaggedFeatureName, Header],
      parentPath: String,
      outputParts: Option[Number],
      endTimeOpt: Option[String],
      saveSchemaMeta: Boolean,
      df: DataFrame,
      header: Header,
      dataLoaderHandlers: List[DataLoaderHandler]): (DataFrame, Header) = {

    val dataPath = FeatureGenerationPathName.getDataPath(parentPath, endTimeOpt)
    val metaPath = FeatureGenerationPathName.getMetaPath(parentPath, endTimeOpt)
    val schemaPath = FeatureGenerationPathName.getSchemaPath(parentPath, endTimeOpt)

    // 1. write feature data
    // Create the temp folder out of the time part.
    // It will generate generatedFeature/_temp_/daily/2020/08/19. So that it won't interfere with the real dataset under generatedFeature/daily/
    val tempParentPath = parentPath + "/_temp_"
    val tempDataPath = FeatureGenerationPathName.getDataPath(tempParentPath, endTimeOpt)
    val numPartsParams = outputParts.map(numParts => Map(SparkIOUtils.OUTPUT_PARALLELISM -> numParts.intValue().toString))
    val parameters = Map(SparkIOUtils.OVERWRITE_MODE -> "ALL") ++ numPartsParams.getOrElse(Map.empty[String, String])

    SourceUtils.safeWriteDF(df, tempDataPath, parameters, dataLoaderHandlers)
    val outputDF = df

    HdfsUtils.hdfsCreateDirectoriesAsNeeded(dataPath)
    HdfsUtils.deletePath(dataPath, true)
    if (HdfsUtils.exists(tempDataPath) && !HdfsUtils.renamePath(tempDataPath, dataPath)) {
      throw new FeathrDataOutputException(
        ErrorLabel.FEATHR_ERROR,
        s"Trying to rename temp path to target path." +
          s"Rename ${tempDataPath} to ${dataPath} failed" +
          s"This is likely a system error. Please retry.")
    }
    HdfsUtils.deletePath(tempParentPath, recursive = true)

    if (saveSchemaMeta) {
      // 2. write schema
      val keyColumns = FeatureGenUtils.getKeyColumnsFromHeader(header)
      val keySqlSchema = outputDF.select(keyColumns.head, keyColumns.tail: _*).schema
      val valueColumns = outputDF.columns.filter(!keyColumns.contains(_))
      // prepare AVRO schema for all key tags/alias
      val keyAvroSchema = FeatureDataHDFSProcessUtils.convertToAvroSchema(keySqlSchema)

      val valueSqlSchema = outputDF.select(valueColumns.head, valueColumns.tail: _*).schema
      val valueAvroSchema = FeatureDataHDFSProcessUtils.convertToAvroSchema(valueSqlSchema)

      val schema = makeOutputSchema(keyAvroSchema, valueAvroSchema)
      FeatureDataHDFSProcessUtils.writeSchemaToDisk(ss, schema, schemaPath)

      // 3. write header data
      writeHeaderDataToDF(ss, featureHeaderMap, metaPath)
    }
    (outputDF, header)
  }

  /**
   * create the output schema of FDS format, this needs to be used to create Redis Store
   * in particular, it creates a field name 'key' that wraps all the key tags
   * and creates a field name 'featureList' that wraps all the feature columns with raw datatyeps
   *
   * @param keySchema key schema
   * @return output schema
   */
  private def makeOutputSchema(keySchema: Schema, valueSchema: Schema): Schema = {
    val outputSchema = Schema.createRecord("AnonRecord_" + Integer.toHexString(valueSchema.hashCode), null, null, false)
    // Avro forbids reusing the Field object. We must make new instances of them.
    val outputKeySchemaField = makeSingleWrappedSchema(keySchema, "keyTags", keySchemaFieldName)
    val outputValueSchemaField = makeSingleWrappedSchema(valueSchema, "features", valueSchemaFieldZName)
    outputSchema.setFields(Seq(outputKeySchemaField, outputValueSchemaField))
    outputSchema
  }

  // single key does not have to be record?
  private def makeSingleWrappedSchema(schema: Schema, recordName: String, wrapperName: String): Schema.Field = {
    val outputKeySchemaFields = schema.getFields.map(f => {
      AvroCompatibilityHelper.createSchemaField(f.name(), f.schema(), f.doc(), SourceUtils.getDefaultValueFromAvroRecord(f), f.order())
    })
    val outputKeySchema = Schema.createRecord(recordName, null, null, false)
    outputKeySchema.setFields(outputKeySchemaFields)
    AvroCompatibilityHelper.createSchemaField(wrapperName, outputKeySchema, null, null)
  }
}
